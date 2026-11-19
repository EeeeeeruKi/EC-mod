package io.github.eeeeeeruki.moremekaponder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.eeeeeeruki.ponderererer.api.FX;
import io.github.eeeeeeruki.ponderererer.world.MekanismSides;
import io.github.eeeeeeruki.ponderererer.element.BeamElement;
import io.github.eeeeeeruki.ponderererer.element.PlayerElement;
import java.util.ArrayList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Vector3f;
import java.util.List;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.InputElementBuilder;
import net.createmod.ponder.api.element.PonderElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Plays one scene from scenes.json: text steps and simple animated actions. */
final class SceneRunner {
    private static final BlockPos CENTER = new BlockPos(3, 1, 3);
    private static final Vec3 TOP = new Vec3(3.5, 2.0, 3.5);
    private static final int TEXT_TICKS = 80;

    private final SceneBuilder scene;
    private final SceneBuildingUtil util;
    private final FX fx;
    private boolean placed;
    private ElementLink<BeamElement> laser;
    private ElementLink<PlayerElement> player;
    private Vec3 playerAt = Vec3.ZERO;
    private final java.util.Map<EquipmentSlot, ItemStack> worn = new java.util.EnumMap<>(EquipmentSlot.class);
    private ElementLink<EntityElement> mob;
    private Vec3 mobPos = Vec3.ZERO;
    private final List<ElementLink<? extends PonderElement>> transient_ = new ArrayList<>();
    private final List<ElementLink<EntityElement>> drops = new ArrayList<>();
    private BlockPos crafter;
    private int itemSpot;

    private SceneRunner(SceneBuilder scene, SceneBuildingUtil util) {
        this.scene = scene;
        this.util = util;
        this.fx = FX.of(scene);
    }

    static void run(JsonObject data, SceneBuilder scene, SceneBuildingUtil util) {
        new SceneRunner(scene, util).play(data);
    }

    private void play(JsonObject data) {
        scene.title(data.get("id").getAsString(), "");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 3, 6), Direction.DOWN);
        if (data.get("main").isJsonNull()) {
            ItemStack hero = stack(data.get("item").getAsString(), 1);
            if (!hero.isEmpty()) scene.world().createItemEntity(new Vec3(0.5, 1.3, 0.5), Vec3.ZERO, hero);
            scene.idle(10);
        }
        for (JsonElement el : data.getAsJsonArray("steps")) {
            JsonObject step = el.getAsJsonObject();
            if (step.has("t")) {
                text();
            } else {
                clearTransient();
                for (JsonElement op : step.getAsJsonArray("a")) {
                    try {
                        op(op.getAsJsonObject());
                    } catch (RuntimeException e) {
                        scene.idle(10); // never let one odd step break the whole scene
                    }
                }
                scene.idle(10);
            }
        }
        clearTransient();
        scene.idle(10);
        scene.markAsFinished();
    }

    private void text() {
        var t = scene.overlay().showText(TEXT_TICKS - 10).text("").attachKeyFrame();
        if (transient_.isEmpty() && placed) t.pointAt(TOP).placeNearTarget();
        else t.independent(10);
        scene.idle(TEXT_TICKS);
    }

    private void clearTransient() {
        for (var link : transient_) fx.hide(link, 6);
        for (var d : drops) scene.world().modifyEntity(d, Entity::discard);
        if (!transient_.isEmpty()) scene.idle(8);
        transient_.clear();
        drops.clear();
    }

    /** Puts a block in place the way a player would: it just appears, with its place sound. */
    private void place(BlockPos pos, BlockState state) {
        scene.world().setBlock(pos, face(state), false);
        fx.sound(state.getSoundType().getPlaceSound(), 1f, 0.8f);
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("energy_cube")) {
            // energy cubes: show every side as an output (looks only)
            fx.setSideConfig(pos, MekanismSides.ENERGY, MekanismSides.OUTPUT);
        }
        fx.connectTransmitters(util.select().fromTo(0, 1, 0, 6, 3, 6));
        if (pos.equals(CENTER)) placed = true;
    }

    private static BlockState face(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p instanceof DirectionProperty dp && p.getName().equals("facing") && dp.getPossibleValues().contains(Direction.NORTH)) {
                // Ponder's default camera looks at the north and west faces, so the front faces north
                return state.setValue(dp, Direction.NORTH);
            }
        }
        return state;
    }

    private void setActive(BlockPos pos, boolean on) {
        scene.world().modifyBlock(pos, st -> {
            for (Property<?> p : st.getProperties()) {
                if (p instanceof BooleanProperty bp && (p.getName().equals("active") || p.getName().equals("lit"))) return st.setValue(bp, on);
            }
            return st;
        }, false);
    }

    private void op(JsonObject o) {
        switch (o.get("op").getAsString()) {
            case "block" -> {
                BlockPos pos = pos(o.getAsJsonArray("pos"));
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                place(pos, state);
                scene.idle(8);
            }
            case "line" -> {
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    BlockPos pos = pos(c.getAsJsonArray());
                    place(pos, state);
                    scene.idle(3);
                }
                scene.idle(6);
            }
            case "swap" -> {
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                if (placed) scene.world().destroyBlock(CENTER);
                scene.idle(5);
                place(CENTER, state);
                scene.idle(15);
            }
            case "swapcells" -> {
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                for (JsonElement c : o.getAsJsonArray("cells")) scene.world().setBlock(pos(c.getAsJsonArray()), state, true);
                fx.connectTransmitters(util.select().fromTo(0, 1, 0, 6, 3, 6));
                scene.idle(15);
            }
            case "machineon" -> {
                setActive(pos(o.getAsJsonArray("pos")), true);
                scene.idle(20);
            }
            case "breakat" -> {
                scene.idle(25);
                scene.world().destroyBlock(pos(o.getAsJsonArray("pos")));
                scene.idle(10);
            }
            case "panel" -> panel(o);
            case "gauge" -> {
                List<Integer> colors = new ArrayList<>();
                for (JsonElement g : o.getAsJsonArray("gauges")) colors.add(g.getAsJsonArray().get(1).getAsInt());
                flow(colors, o.get("drain").getAsBoolean());
                setActive(CENTER, true);
                scene.idle(40);
                setActive(CENTER, false);
            }
            case "laser" -> {
                if (laser != null) return;
                laser = fx.laserShoot(vec(o.getAsJsonArray("from")), vec(o.getAsJsonArray("to")), 0xFFFF3030,
                        o.get("width").getAsFloat(), 10);
                scene.idle(20);
            }
            case "laseroff" -> {
                if (laser != null) fx.hide(laser, 5);
                laser = null;
                scene.idle(10);
            }
            case "lightning" -> {
                fx.zap(new Vec3(3.5, 2.6, 3.5), new Vec3(3.5, 1.6, 3.5), 0xFFB48CFF, 25);
                scene.idle(30);
            }
            case "explode" -> {
                scene.idle(20);
                fx.explosion(new Vec3(3.5, 1.5, 3.5), o.get("r").getAsFloat(), o.get("break").getAsBoolean());
                scene.idle(20);
            }
            case "player" -> player(o);
            case "mine" -> mine(o);
            case "click" -> {
                Vec3 at = o.has("at") ? vec(o.getAsJsonArray("at")) : TOP;
                InputElementBuilder in = scene.overlay().showControls(at, Pointing.DOWN, 40);
                in = o.get("left").getAsBoolean() ? in.leftClick() : in.rightClick();
                if (o.get("sneak").getAsBoolean()) in = in.whileSneaking();
                if (o.has("item") && !o.get("item").isJsonNull()) {
                    ItemStack held = stack(o.get("item").getAsString(), 1);
                    if (!held.isEmpty()) in.withItem(held);
                }
                scene.idle(45);
            }
            case "mob" -> {
                Vec3 at = o.has("pos") ? vec(o.getAsJsonArray("pos")) : new Vec3(5.5, 1, 1.5);
                mobPos = at;
                String type = o.get("type").getAsString();
                if (type.contains(":") && !type.startsWith("minecraft:")) {
                    // modded mobs (the Robit) need data a Ponder world can't give them: show the item instead
                    ItemStack s = stack(type, 1);
                    if (s.isEmpty()) return;
                    mob = scene.world().createItemEntity(at.add(0, 0.3, 0), Vec3.ZERO, s);
                } else {
                    EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.get(rl(type));
                    mob = scene.world().createEntity(level -> {
                        Entity e = et.create(level);
                        if (e == null) e = EntityType.PIG.create(level);
                        e.setPos(at.x, at.y, at.z);
                        e.setYRot(180);
                        return e;
                    });
                }
                scene.idle(20);
            }
            case "mobact" -> mobAct(o.get("kind").getAsString());
            case "show" -> {
                for (JsonElement id : o.getAsJsonArray("ids")) {
                    Vec3 spot = nextSpot();
                    BlockState bs = block(id.getAsString());
                    if (bs != null && bs.getBlock().asItem() != Items.AIR && !(bs.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) {
                        place(BlockPos.containing(spot.x, 1, spot.z), bs);
                    } else {
                        ItemStack s = stack(id.getAsString(), 1);
                        if (s.isEmpty()) continue;
                        drops.add(scene.world().createItemEntity(spot, Vec3.ZERO, s));
                    }
                    scene.idle(6);
                }
                scene.idle(20);
            }
            default -> { // pulse
                if (placed) scene.effects().indicateSuccess(CENTER);
                scene.idle(25);
            }
        }
    }

    /** Shows a machine at work in the world: items go in on top, the machine runs, results pop out the front. */
    private void panel(JsonObject o) {
        BlockPos target = CENTER;
        String title = o.get("title").isJsonNull() ? null : o.get("title").getAsString();
        if ("minecraft:crafting_table".equals(title)) {
            if (crafter == null) {
                crafter = new BlockPos(5, 1, 1);
                place(crafter, Blocks.CRAFTING_TABLE.defaultBlockState());
                scene.idle(10);
            }
            target = crafter;
        } else if (o.has("at") && !o.get("at").isJsonNull()) {
            target = pos(o.getAsJsonArray("at")); // a second machine beside the scene's own block
        } else if (!placed && title != null) {
            BlockState st = block(title);
            if (st != null) { place(CENTER, st); scene.idle(10); }
        }
        Vec3 top = Vec3.atCenterOf(target).add(0, 1.1, 0);
        for (JsonElement e : o.getAsJsonArray("in")) {
            JsonArray a = e.getAsJsonArray();
            ItemStack s = stack(a.get(0).getAsString(), a.get(1).getAsInt());
            if (s.isEmpty()) continue;
            ElementLink<EntityElement> link = scene.world().createItemEntity(top.add(0, 0.6, 0), new Vec3(0, -0.05, 0), s);
            scene.idle(12);
            scene.world().modifyEntity(link, Entity::discard);
            scene.idle(4);
        }
        List<Integer> colors = new ArrayList<>();
        if (o.has("gauge") && !o.get("gauge").isJsonNull())
            for (JsonElement g : o.getAsJsonArray("gauge")) colors.add(g.getAsJsonArray().get(1).getAsInt());
        boolean drain = o.has("drain") && o.get("drain").getAsBoolean();
        if (!colors.isEmpty()) flow(colors, drain);
        setActive(target, true);
        scene.idle(40);
        setActive(target, false);
        Vec3 front = Vec3.atCenterOf(target).add(0, -0.1, -0.65);
        for (JsonElement e : o.getAsJsonArray("out")) {
            JsonArray a = e.getAsJsonArray();
            ItemStack s = stack(a.get(0).getAsString(), a.get(1).getAsInt());
            if (s.isEmpty()) continue;
            drops.add(scene.world().createItemEntity(front, new Vec3(0, 0.18, -0.12), s));
            scene.idle(6);
        }
        scene.idle(25);
    }

    /** Chemicals and fluids: coloured particles running through the tube or pipe into the machine. */
    private void flow(List<Integer> colors, boolean out) {
        Vec3 c = Vec3.atCenterOf(CENTER);
        Vec3 tube = new Vec3(3.5, 1.5, 6.5);
        for (int col : colors) {
            Vector3f rgb = new Vector3f(((col >> 16) & 255) / 255f, ((col >> 8) & 255) / 255f, (col & 255) / 255f);
            DustParticleOptions dust = new DustParticleOptions(rgb, 1.2f);
            for (int i = 0; i < 3; i++) {
                if (out) fx.particleLine(dust, c, c.add(0, 1.2, 0), 8);
                else fx.particleLine(dust, tube, c, 12);
                scene.idle(6);
            }
        }
    }

    /** The player figure, made once per scene; later steps move it instead of making a new one. */
    private void ensurePlayer(Vec3 feet, float yaw) {
        if (player == null) {
            player = fx.player(feet, yaw);
            playerAt = feet;
            scene.idle(10);
        } else if (!feet.equals(playerAt)) {
            fx.playerPose(player, PlayerElement.PlayerPose.WALK);
            fx.playerMove(player, feet, 20);
            scene.idle(22);
            fx.playerPose(player, PlayerElement.PlayerPose.STAND);
            playerAt = feet;
        }
        fx.playerTurn(player, yaw, 5);
    }

    /** Puts gear on piece by piece, the way a player dresses: a swing and the equip sound for each piece. */
    private void wear(JsonArray equip) {
        for (JsonElement e : equip) {
            JsonArray a = e.getAsJsonArray();
            EquipmentSlot slot = EquipmentSlot.valueOf(a.get(0).getAsString());
            ItemStack s = stack(a.get(1).getAsString(), 1);
            if (s.isEmpty() || ItemStack.isSameItem(s, worn.getOrDefault(slot, ItemStack.EMPTY))) continue;
            worn.put(slot, s);
            if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
                fx.playerEquip(player, slot, s);
                continue;
            }
            fx.playerSwing(player);
            fx.playerEquip(player, slot, s);
            fx.sound(s.getItem() instanceof ArmorItem armor ? armor.getMaterial().value().equipSound() : SoundEvents.ARMOR_EQUIP_GENERIC, 1f, 1f);
            scene.idle(8);
        }
    }

    private void player(JsonObject o) {
        Vec3 feet = o.has("at") ? vec(o.getAsJsonArray("at")) : new Vec3(0.5, 1, 5.5);
        ensurePlayer(feet, 135);
        fx.playerPose(player, PlayerElement.PlayerPose.STAND);
        if (o.has("sets")) {
            // several armor sets: the player changes from one set into the next
            for (JsonElement set : o.getAsJsonArray("sets")) {
                wear(set.getAsJsonArray());
                fx.playerTurn(player, 135 + 180, 20);
                scene.idle(22);
                fx.playerTurn(player, 135 + 360, 20);
                scene.idle(25);
            }
            return;
        }
        wear(o.getAsJsonArray("equip"));
        PlayerElement.PlayerPose pose = PlayerElement.PlayerPose.valueOf(o.get("pose").getAsString());
        fx.playerPose(player, pose);
        scene.idle(10);
        if (o.get("move").getAsBoolean()) {
            fx.playerTurn(player, -90, 5);
            fx.playerMove(player, feet.add(2, 0, 0), pose == PlayerElement.PlayerPose.SPRINT ? 20 : 40);
            playerAt = feet.add(2, 0, 0);
            scene.idle(45);
        } else if (o.get("rise").getAsBoolean()) {
            fx.playerMove(player, feet.add(0, 2.5, 0), 40);
            playerAt = feet.add(0, 2.5, 0);
            fx.particleLine(ParticleTypes.FLAME, feet.add(0, 0.6, 0), feet.add(0, 2.2, 0), 10);
            scene.idle(45);
        } else if (pose == PlayerElement.PlayerPose.USE_ITEM) {
            fx.playerSwing(player);
            scene.idle(40);
        } else {
            scene.idle(30);
        }
    }

    /**
     * A player breaking blocks with a tool: swings while the crack grows, then the block breaks.
     * mode: each (one by one), fast (one by one, quickly), all (every block at once), vein (one break takes the whole cluster), one.
     */
    private void mine(JsonObject o) {
        for (JsonElement e : o.getAsJsonArray("place")) {
            JsonArray a = e.getAsJsonArray();
            BlockState st = block(a.get(1).getAsString());
            if (st == null) continue;
            place(pos(a.get(0).getAsJsonArray()), st);
            scene.idle(3);
        }
        scene.idle(10);
        List<BlockPos> targets = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("targets")) targets.add(pos(e.getAsJsonArray()));
        if (targets.isEmpty()) return;
        Vec3 stand = vec(o.getAsJsonArray("stand"));
        float yaw = o.get("yaw").getAsFloat();
        ensurePlayer(stand, yaw);
        fx.playerPose(player, PlayerElement.PlayerPose.STAND);
        ItemStack tool = stack(o.get("tool").getAsString(), 1);
        JsonArray hand = new JsonArray();
        JsonArray pair = new JsonArray();
        pair.add("MAINHAND");
        pair.add(o.get("tool").getAsString());
        hand.add(pair);
        wear(hand);
        fx.playerLook(player, 35);
        scene.idle(10);
        String mode = o.get("mode").getAsString();
        int step = mode.equals("fast") || mode.equals("all") || mode.equals("vein") ? 1 : 3;
        boolean sideways = yaw == 180 || yaw == 0;
        switch (mode) {
            case "all" -> {
                breakTogether(targets, step);
                scene.idle(10);
            }
            case "vein" -> {
                BlockPos first = targets.get(0);
                dig(first, step);
                scene.idle(3);
                for (BlockPos p : targets.subList(1, targets.size())) {
                    breakNow(p);
                    scene.idle(2);
                }
                scene.idle(10);
            }
            default -> {
                for (BlockPos p : targets) {
                    if (sideways && player != null) {
                        Vec3 to = new Vec3(p.getX() + 0.5, stand.y, stand.z);
                        if (!to.equals(playerAt)) {
                            fx.playerMove(player, to, 6);
                            playerAt = to;
                            scene.idle(7);
                        }
                    }
                    dig(p, step);
                    scene.idle(mode.equals("fast") ? 3 : 8);
                }
            }
        }
        if (o.has("drop") && !o.get("drop").isJsonNull()) {
            ItemStack s = stack(o.get("drop").getAsString(), 1);
            if (!s.isEmpty()) drops.add(scene.world().createItemEntity(Vec3.atCenterOf(targets.get(0)), new Vec3(0, 0.15, 0), s));
            if (targets.get(0).equals(CENTER)) placed = false;
        }
        fx.playerLook(player, 0);
        scene.idle(20);
    }

    /** Swings at one block until the crack is full and it breaks (the 10th step breaks it and clears the crack). */
    private void dig(BlockPos pos, int ticksPerStage) {
        for (int stage = 0; stage < 10; stage++) {
            if (stage % 2 == 0) fx.playerSwing(player);
            if (stage == 9) breakSound(pos);
            scene.world().incrementBlockBreakingProgress(pos);
            scene.idle(ticksPerStage);
        }
    }

    private void breakTogether(List<BlockPos> targets, int ticksPerStage) {
        for (int stage = 0; stage < 10; stage++) {
            if (stage % 2 == 0) fx.playerSwing(player);
            if (stage == 9) breakSound(targets.get(0));
            for (BlockPos p : targets) scene.world().incrementBlockBreakingProgress(p);
            scene.idle(ticksPerStage);
        }
    }

    private void breakSound(BlockPos pos) {
        scene.addInstruction(ps -> {
            BlockState st = ps.getWorld().getBlockState(pos);
            if (!st.isAir()) ps.addElement(new io.github.eeeeeeruki.ponderererer.element.SoundElement(st.getSoundType().getBreakSound(), 1f, 0.8f));
        });
    }

    private void breakNow(BlockPos pos) {
        breakSound(pos);
        scene.world().destroyBlock(pos);
    }

    private void mobAct(String kind) {
        if (mob == null) return;
        ElementLink<EntityElement> m = mob;
        switch (kind) {
            case "kill" -> {
                scene.idle(20);
                scene.world().modifyEntity(m, Entity::discard);
                fx.particleLine(ParticleTypes.POOF, mobPos.add(0, 0.2, 0), mobPos.add(0, 0.8, 0), 8);
                mob = null;
                scene.idle(20);
            }
            case "float" -> {
                for (int i = 0; i < 20; i++) {
                    scene.world().modifyEntity(m, e -> e.setPos(e.getX(), e.getY() + 0.08, e.getZ()));
                    scene.idle(2);
                }
            }
            default -> { // move to the center
                double dx = (3.5 - mobPos.x) / 20, dz = (3.5 - mobPos.z) / 20;
                for (int i = 0; i < 20; i++) {
                    scene.world().modifyEntity(m, e -> e.setPos(e.getX() + dx, e.getY() + (e.getY() < 1.15 ? 0.01 : 0), e.getZ() + dz));
                    scene.idle(2);
                }
            }
        }
        scene.idle(10);
    }

    private Vec3 nextSpot() {
        // free cells in front of the machine, away from cables, pipes, the crafting table and the player
        Vec3[] spots = {new Vec3(1.5, 1.3, 1.5), new Vec3(2.5, 1.3, 1.5), new Vec3(1.5, 1.3, 0.5), new Vec3(2.5, 1.3, 0.5),
                new Vec3(0.5, 1.3, 1.5), new Vec3(4.5, 1.3, 0.5)};
        return spots[itemSpot++ % spots.length];
    }

    private static ResourceLocation rl(String id) {
        return ResourceLocation.parse(id.contains(":") ? id : "minecraft:" + id);
    }

    private static BlockState block(String id) {
        Block b = BuiltInRegistries.BLOCK.get(rl(id));
        return b == Blocks.AIR ? null : b.defaultBlockState();
    }

    private static ItemStack stack(String id, int count) {
        Item item = BuiltInRegistries.ITEM.get(rl(id));
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, count));
    }

    private static Component name(String id) {
        if (id == null) return Component.empty();
        ItemStack s = stack(id, 1);
        return s.isEmpty() ? Component.empty() : s.getHoverName();
    }

    private static BlockPos pos(JsonArray a) {
        return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
    }

    private static Vec3 vec(JsonArray a) {
        return new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
    }
}
