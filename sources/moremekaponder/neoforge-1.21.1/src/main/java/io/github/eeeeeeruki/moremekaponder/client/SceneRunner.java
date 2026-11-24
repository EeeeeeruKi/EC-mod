package io.github.eeeeeeruki.moremekaponder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.eeeeeeruki.ponderererer.api.CardBuilder;
import io.github.eeeeeeruki.ponderererer.api.FX;
import io.github.eeeeeeruki.ponderererer.element.CardElement;
import io.github.eeeeeeruki.ponderererer.element.LabelElement;
import io.github.eeeeeeruki.ponderererer.element.SpinElement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import io.github.eeeeeeruki.ponderererer.world.MekanismSides;
import io.github.eeeeeeruki.ponderererer.element.BeamElement;
import io.github.eeeeeeruki.ponderererer.element.PlayerElement;
import java.util.ArrayList;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
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
import net.minecraft.sounds.SoundEvent;
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
    private Vec3 moveTo = new Vec3(3.5, 1, 3.5);
    private final List<ElementLink<BeamElement>> beams = new ArrayList<>();
    private Vec3 playerAt = Vec3.ZERO;
    private final java.util.Map<EquipmentSlot, ItemStack> worn = new java.util.EnumMap<>(EquipmentSlot.class);
    private ElementLink<EntityElement> mob;
    private Vec3 mobPos = Vec3.ZERO;
    private final List<ElementLink<? extends PonderElement>> transient_ = new ArrayList<>();
    private final List<ElementLink<EntityElement>> drops = new ArrayList<>();
    private BlockPos crafter;
    private int itemSpot;
    private String centerId;
    private final Map<String, ElementLink<CardElement>> cards = new HashMap<>();
    private final Map<String, ElementLink<LabelElement>> labels = new HashMap<>();
    private final Map<String, ElementLink<SpinElement>> spins = new HashMap<>();
    private static final int ENERGY = 0xFF3CFE9A, DARK = 0xFF404040;

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
        if (data.has("zoom")) scene.scaleSceneView(data.get("zoom").getAsFloat());
        if (data.has("offsetY")) scene.setSceneOffsetY(data.get("offsetY").getAsFloat());
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 4, 6), Direction.DOWN);
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
        place(pos, state, false);
    }

    private void place(BlockPos pos, BlockState state, boolean keepFacing) {
        scene.world().setBlock(pos, keepFacing ? state : face(state), false);
        fx.sound(state.getSoundType().getPlaceSound(), 1f, 0.8f);
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().endsWith("energy_cube")) {
            // energy cubes: show every side as an output (looks only)
            fx.setSideConfig(pos, MekanismSides.ENERGY, MekanismSides.OUTPUT);
        }
        fx.connectTransmitters(util.select().fromTo(0, 1, 0, 6, 4, 6));
        if (pos.equals(CENTER)) {
            placed = true;
            centerId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        }
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
                if (state == null && o.has("alt")) state = block(o.get("alt").getAsString()); // an addon block, when the addon is missing
                if (state == null) return;
                boolean keep = o.has("facing") || o.has("props");
                place(pos, props(state, o), keep);
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 8);
            }
            case "floor" -> {
                // blocks in the ground (y = 0): ores, water, holes
                String id = o.get("id").getAsString();
                BlockState state = id.equals("minecraft:air") ? Blocks.AIR.defaultBlockState() : block(id);
                if (state == null) return;
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    JsonArray a = c.getAsJsonArray();
                    scene.world().setBlock(new BlockPos(a.get(0).getAsInt(), 0, a.get(1).getAsInt()), state, false);
                    if (o.has("step")) scene.idle(o.get("step").getAsInt());
                }
                scene.idle(10);
            }
            case "remove" -> {
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    BlockPos p = pos(c.getAsJsonArray());
                    scene.world().setBlock(p, Blocks.AIR.defaultBlockState(), false);
                    if (p.equals(CENTER)) placed = false;
                }
                scene.idle(5);
            }
            case "destroy" -> {
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    BlockPos p = pos(c.getAsJsonArray());
                    breakNow(p);
                    if (p.equals(CENTER)) placed = false;
                    if (o.has("step")) scene.idle(o.get("step").getAsInt());
                }
                scene.idle(10);
            }
            case "carry" -> carry(o);
            case "clear" -> clearTransient(); // between two recipes shown in one step
            case "card" -> card(o);
            case "cardslot" -> {
                var link = cards.get(o.get("id").getAsString());
                if (link == null) return;
                ItemStack st = o.has("item") && !o.get("item").isJsonNull() ? stackOf(o.getAsJsonArray("item")) : ItemStack.EMPTY;
                fx.cardSlot(link, o.get("slot").getAsInt(), st);
                if (o.has("wait")) scene.idle(o.get("wait").getAsInt());
            }
            case "cardbar" -> {
                var link = cards.get(o.get("id").getAsString());
                if (link == null) return;
                int t = o.has("ticks") ? o.get("ticks").getAsInt() : 30;
                fx.cardBar(link, o.get("bar").getAsInt(), o.get("value").getAsFloat(), t);
                if (o.has("wait")) scene.idle(o.get("wait").getAsInt());
            }
            case "cardprogress" -> {
                var link = cards.get(o.get("id").getAsString());
                if (link == null) return;
                int t = o.has("ticks") ? o.get("ticks").getAsInt() : 30;
                fx.cardProgress(link, o.get("value").getAsFloat(), t);
                if (o.has("wait")) scene.idle(o.get("wait").getAsInt());
            }
            case "cardtext" -> {
                var link = cards.get(o.get("id").getAsString());
                if (link == null) return;
                fx.cardText(link, o.get("line").getAsInt(), text(o.get("text")));
                if (o.has("wait")) scene.idle(o.get("wait").getAsInt());
            }
            case "cardoff" -> {
                var link = cards.remove(o.get("id").getAsString());
                if (link != null) fx.hide(link, 6);
                scene.idle(6);
            }
            case "label" -> {
                String id = o.has("id") ? o.get("id").getAsString() : "label" + labels.size();
                int col = o.has("color") ? argb(o.get("color").getAsInt()) : 0xFFFFFFFF;
                var link = fx.label(vec(o.getAsJsonArray("at")), text(o.get("text")), col);
                labels.put(id, link);
                if (!(o.has("keep") && o.get("keep").getAsBoolean())) transient_.add(link);
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
            }
            case "labeloff" -> {
                var link = labels.remove(o.get("id").getAsString());
                if (link != null) fx.hide(link, 6);
                scene.idle(6);
            }
            case "tag" -> {
                try {
                    CompoundTag tag = TagParser.parseTag(o.get("nbt").getAsString());
                    for (JsonElement c : o.getAsJsonArray("cells")) fx.updateTag(pos(c.getAsJsonArray()), tag);
                } catch (Exception ignored) {
                }
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
            }
            case "paint" -> {
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    fx.transporterColor(pos(c.getAsJsonArray()), o.get("color").getAsInt());
                    if (o.has("step")) scene.idle(o.get("step").getAsInt());
                }
                scene.idle(10);
            }
            case "spin" -> {
                List<String> fields = new ArrayList<>();
                for (JsonElement f : o.getAsJsonArray("fields")) fields.add(f.getAsString());
                spins.put(o.get("id").getAsString(), fx.spin(pos(o.getAsJsonArray("pos")), o.get("speed").getAsFloat(), fields.toArray(new String[0])));
            }
            case "spinspeed" -> {
                var link = spins.get(o.get("id").getAsString());
                if (link != null) fx.setSpin(link, o.get("speed").getAsFloat());
            }
            case "spinoff" -> {
                var link = spins.remove(o.get("id").getAsString());
                if (link != null) fx.hide(link, 0);
            }
            case "dark" -> {
                fx.darkness(o.get("sky").getAsInt());
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 10);
            }
            case "light" -> {
                fx.lightSource(pos(o.getAsJsonArray("pos")), o.get("level").getAsInt());
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
            }
            case "lightoff" -> {
                fx.clearLight();
                scene.idle(10);
            }
            case "tagcard" -> tagCard(o);
            case "blocks" -> {
                // many blocks at once: [[x, y, z], id] pairs, one place sound
                SoundEvent sound = null;
                for (JsonElement e : o.getAsJsonArray("list")) {
                    JsonArray a = e.getAsJsonArray();
                    BlockState st = block(a.get(1).getAsString());
                    if (st == null) continue;
                    scene.world().setBlock(pos(a.get(0).getAsJsonArray()), face(st), false);
                    if (sound == null) sound = st.getSoundType().getPlaceSound();
                    if (o.has("step")) scene.idle(o.get("step").getAsInt());
                }
                if (sound != null) fx.sound(sound, 1f, 0.8f);
                fx.connectTransmitters(util.select().fromTo(0, 1, 0, 6, 4, 6));
                scene.idle(10);
            }
            case "sound" -> {
                SoundEvent se = BuiltInRegistries.SOUND_EVENT.get(rl(o.get("id").getAsString()));
                if (se != null) fx.sound(se, 1f, 1f);
            }
            case "stream" -> stream(o);
            case "active" -> {
                setActive(pos(o.getAsJsonArray("pos")), o.get("on").getAsBoolean());
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 10);
            }
            case "lever" -> {
                BlockPos p = pos(o.getAsJsonArray("pos"));
                boolean on = o.get("on").getAsBoolean();
                scene.world().modifyBlock(p, st -> st.hasProperty(BlockStateProperties.POWERED) ? st.setValue(BlockStateProperties.POWERED, on) : st, false);
                fx.sound(SoundEvents.LEVER_CLICK, 0.6f, on ? 0.6f : 0.5f);
                scene.idle(15);
            }
            case "beam" -> {
                int col = o.has("color") ? o.get("color").getAsInt() : 0xFFFF3030;
                beams.add(fx.laserShoot(vec(o.getAsJsonArray("from")), vec(o.getAsJsonArray("to")), col,
                        o.has("width") ? o.get("width").getAsFloat() : 0.06f, 10));
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 15);
            }
            case "beamsoff" -> {
                for (var b : beams) fx.hide(b, 5);
                beams.clear();
                scene.idle(10);
            }
            case "actor" -> actor(o);
            case "indicate" -> {
                scene.effects().indicateSuccess(pos(o.getAsJsonArray("pos")));
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 15);
            }
            case "item" -> {
                ItemStack s = stack(o.get("id").getAsString(), o.has("count") ? o.get("count").getAsInt() : 1);
                if (s.isEmpty()) return;
                Vec3 motion = o.has("motion") ? vec(o.getAsJsonArray("motion")) : Vec3.ZERO;
                ElementLink<EntityElement> link = scene.world().createItemEntity(vec(o.getAsJsonArray("at")), motion, s);
                if (o.has("float") && o.get("float").getAsBoolean()) {
                    scene.world().modifyEntity(link, e -> {
                        e.setNoGravity(true);
                        e.setDeltaMovement(Vec3.ZERO);
                    });
                }
                drops.add(link);
                scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 6);
            }
            case "wait" -> scene.idle(o.get("ticks").getAsInt());
            case "flash" -> {
                fx.flash(o.has("color") ? o.get("color").getAsInt() : 0x60FFFFFF, 10);
                scene.idle(10);
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
                if (o.has("from") && uiWanted(o)) {
                    CardBuilder b = CardBuilder.of(Component.translatable("moremekaponder.ui.cable_upgrade"))
                            .icon(stack(o.get("id").getAsString(), 1));
                    JsonArray first = o.getAsJsonArray("cells").get(0).getAsJsonArray();
                    b.target(Vec3.atCenterOf(pos(first)));
                    ItemStack from = stack(o.get("from").getAsString(), 1), to = stack(o.get("id").getAsString(), 1);
                    ItemStack held = o.has("held") ? stack(o.get("held").getAsString(), 1) : ItemStack.EMPTY;
                    int a = b.slot(from), h = b.slot(held), t = b.bigSlot(to);
                    b.process(new int[0], held.isEmpty() ? new int[]{a} : new int[]{a, h}, true, new int[]{t}, new int[0]);
                    b.text(caption(held.isEmpty() ? List.of(from) : List.of(from, held), List.of(to), null));
                    transient_.add(fx.card(b));
                }
                for (JsonElement c : o.getAsJsonArray("cells")) scene.world().setBlock(pos(c.getAsJsonArray()), state, true);
                fx.connectTransmitters(util.select().fromTo(0, 1, 0, 6, 4, 6));
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
                List<Gauge> gs = gauges(o.getAsJsonArray("gauges"));
                boolean drain = o.get("drain").getAsBoolean();
                BlockPos at = o.has("at") ? pos(o.getAsJsonArray("at")) : CENTER;
                Ui card = uiWanted(o) ? gaugeCard(o, at.equals(CENTER) ? centerId : null, at, gs, drain) : null;
                if (!(o.has("particles") && !o.get("particles").getAsBoolean()))
                    flow(gs, at, o.has("src") ? vec(o.getAsJsonArray("src")) : null, false);
                setActive(at, true);
                if (card != null) card.run(40);
                scene.idle(40);
                setActive(at, false);
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
                fx.explosion(o.has("at") ? vec(o.getAsJsonArray("at")) : new Vec3(3.5, 1.5, 3.5), o.get("r").getAsFloat(), o.get("break").getAsBoolean());
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
            case "mobact" -> {
                moveTo = o.has("to") ? vec(o.getAsJsonArray("to")) : new Vec3(3.5, 1, 3.5);
                mobAct(o.get("kind").getAsString());
            }
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
        boolean crafting = "minecraft:crafting_table".equals(title);
        if (crafting) {
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
        String machine = title != null ? title : centerId;
        Vec3 top = o.has("top") ? new Vec3(target.getX() + 0.5, o.get("top").getAsDouble(), target.getZ() + 0.5)
                : Vec3.atCenterOf(target).add(0, 1.1, 0);
        List<ItemStack> ins = stacks(o.getAsJsonArray("in"));
        List<ItemStack> outs = stacks(o.getAsJsonArray("out"));
        List<Gauge> gauges = gauges(o.has("gauge") && !o.get("gauge").isJsonNull() ? o.getAsJsonArray("gauge") : null);
        boolean drain = o.has("drain") && o.get("drain").getAsBoolean();
        Ui card = null;
        if (uiWanted(o)) card = crafting ? craftCard(top, ins, outs) : machineCard(o, machine, top, ins, outs, gauges, drain);
        for (int i = 0; i < ins.size(); i++) {
            ItemStack s = ins.get(i);
            ElementLink<EntityElement> link = scene.world().createItemEntity(top.add(0, 0.6, 0), new Vec3(0, -0.05, 0), s);
            scene.idle(12);
            scene.world().modifyEntity(link, Entity::discard);
            if (card != null) card.put(i, s);
            scene.idle(4);
        }
        if (!gauges.isEmpty()) flow(gauges, target, o.has("src") ? vec(o.getAsJsonArray("src")) : null, !ins.isEmpty());
        setActive(target, true);
        if (card != null) card.run(crafting ? 20 : 40);
        scene.idle(crafting ? 20 : 40);
        setActive(target, false);
        if (card != null) card.finish(outs);
        Vec3 front = o.has("front") ? vec(o.getAsJsonArray("front")) : Vec3.atCenterOf(target).add(0, -0.1, -0.65);
        Vec3 push = o.has("push") ? vec(o.getAsJsonArray("push")) : new Vec3(0, 0.18, -0.12);
        for (ItemStack s : outs) {
            drops.add(scene.world().createItemEntity(front, push, s));
            scene.idle(6);
        }
        scene.idle(25);
    }

    /** A gauge of a machine: what it holds, its colour, and whether it goes in (used up) or comes out (made). */
    private record Gauge(Component label, int color, boolean in) {
    }

    private static List<Gauge> gauges(JsonArray a) {
        List<Gauge> out = new ArrayList<>();
        if (a == null) return out;
        for (JsonElement e : a) {
            JsonArray g = e.getAsJsonArray();
            boolean in = g.size() > 2 && "in".equals(g.get(2).getAsString());
            out.add(new Gauge(text(g.get(0)), g.get(1).getAsInt(), in));
        }
        return out;
    }

    /** Chemicals and fluids: coloured particles running from the tube into the machine, or rising out of it. */
    private void flow(List<Gauge> gauges, BlockPos at, Vec3 src, boolean fedByItem) {
        Vec3 c = Vec3.atCenterOf(at);
        Vec3 tube = src != null ? src : new Vec3(6.5, 1.5, 3.5);
        for (Gauge g : gauges) {
            int col = g.color();
            Vector3f rgb = new Vector3f(((col >> 16) & 255) / 255f, ((col >> 8) & 255) / 255f, (col & 255) / 255f);
            DustParticleOptions dust = new DustParticleOptions(rgb, 1.2f);
            boolean rising = !g.in() || (fedByItem && src == null);
            for (int i = 0; i < 3; i++) {
                if (rising) fx.particleLine(dust, c, c.add(0, 1.2, 0), 8);
                else fx.particleLine(dust, tube, c, 12);
                scene.idle(6);
            }
        }
    }

    // ------------------------------------------------------------------ cards (the on-screen UI)

    /** A card on screen and what to do with it while the machine runs. */
    private final class Ui {
        ElementLink<CardElement> link;
        int[] in = new int[0], out = new int[0];
        int energy = -1, gen = -1;
        float energyTo = 0.72f;
        final List<int[]> moves = new ArrayList<>(); // {bar, to * 1000}
        final List<Integer> clear = new ArrayList<>(); // slots emptied when the work is done
        List<ItemStack> outOverride;
        boolean consume = true, arrow = true;

        void put(int i, ItemStack s) {
            if (i < in.length && in[i] >= 0) fx.cardSlot(link, in[i], s);
        }

        void run(int ticks) {
            if (arrow) fx.cardProgress(link, 1f, ticks);
            if (energy >= 0) fx.cardBar(link, energy, energyTo, ticks);
            if (gen >= 0) fx.cardBar(link, gen, 0.85f, ticks);
            for (int[] m : moves) fx.cardBar(link, m[0], m[1] / 1000f, ticks);
        }

        void finish(List<ItemStack> outs) {
            if (arrow) fx.cardProgress(link, 0f, 0);
            if (consume) for (int i : in) if (i >= 0) fx.cardSlot(link, i, ItemStack.EMPTY);
            for (int i : clear) fx.cardSlot(link, i, ItemStack.EMPTY);
            List<ItemStack> shown = outOverride != null ? outOverride : outs;
            for (int k = 0; k < out.length && k < shown.size(); k++) fx.cardSlot(link, out[k], shown.get(k));
        }
    }

    private static boolean uiWanted(JsonObject o) {
        return !o.has("ui") || o.get("ui").getAsBoolean();
    }

    private static Component machineName(ItemStack icon) {
        return icon.isEmpty() ? Component.translatable("moremekaponder.ui.machine") : icon.getHoverName();
    }

    private static final int HEAT = 0xFFFF5020, NOTE = 0xFF2A5DB0;

    /** Inputs, a progress arrow and outputs, with the energy and chemical gauges of the machine. */
    private Ui machineCard(JsonObject o, String machine, Vec3 top, List<ItemStack> ins, List<ItemStack> outs,
                           List<Gauge> gauges, boolean drain) {
        ItemStack icon = machine == null ? ItemStack.EMPTY : stack(machine, 1);
        CardBuilder b = CardBuilder.of(machineName(icon)).icon(icon).target(top);
        Ui ui = new Ui();
        String path = machine == null ? "" : rl(machine).getPath();
        boolean cube = path.endsWith("energy_cube");
        boolean tank = path.endsWith("chemical_tank") || path.endsWith("fluid_tank");
        ItemStack upgrade = ins.stream().filter(st -> key(st).getPath().startsWith("upgrade_")).findFirst().orElse(ItemStack.EMPTY);
        boolean sameOut = !outs.isEmpty() && !ins.isEmpty() && ItemStack.isSameItem(outs.get(0), ins.get(0));
        boolean fill = (cube || tank || (o.has("fill") && o.get("fill").getAsBoolean())) && !ins.isEmpty() && upgrade.isEmpty()
                && (outs.isEmpty() || sameOut);
        boolean heater = path.equals("fuelwood_heater") || path.equals("resistive_heater");
        boolean module = path.equals("modification_station");
        List<Integer> left = new ArrayList<>(), right = new ArrayList<>();
        if (o.has("energy") && o.get("energy").getAsBoolean() && !fill) {
            ui.energy = b.bar(ENERGY, 1f, Component.translatable("moremekaponder.ui.energy"));
            left.add(ui.energy);
        }
        if (o.has("gen") && o.get("gen").getAsBoolean()) {
            ui.gen = b.bar(ENERGY, 0.1f, Component.translatable("moremekaponder.ui.generated"));
        }
        if (fill) {
            // a tank or cube filling an item put in its slot
            int slot = b.slot(ItemStack.EMPTY);
            ui.in = new int[]{slot};
            ui.consume = false;
            List<Integer> src = new ArrayList<>(), dst = new ArrayList<>();
            if (cube) {
                int stored = b.bar(ENERGY, 1f, Component.translatable("moremekaponder.ui.energy"));
                int item = b.bar(0xFF2FB36F, 0f, Component.translatable("moremekaponder.ui.item_energy"));
                src.add(stored);
                dst.add(item);
                ui.moves.add(new int[]{stored, 550});
                ui.moves.add(new int[]{item, 1000});
            } else {
                for (Gauge g : gauges) {
                    int stored = b.bar(0xFF000000 | g.color(), 0.9f, g.label());
                    int item = b.bar(0xFF000000 | g.color(), 0f, Component.translatable("moremekaponder.ui.item_contents"));
                    src.add(stored);
                    dst.add(item);
                    ui.moves.add(new int[]{stored, 600});
                    ui.moves.add(new int[]{item, 1000});
                }
            }
            ui.arrow = !src.isEmpty();
            b.process(ints(src), new int[0], ui.arrow, new int[]{slot}, ints(dst));
            b.text(Component.translatable(cube ? "moremekaponder.ui.charging" : "moremekaponder.ui.filling", ins.get(0).getHoverName()));
        } else if (!upgrade.isEmpty()) {
            for (Gauge g : gauges) {
                int bar = b.bar(0xFF000000 | g.color(), drain ? 0.9f : 0.2f, g.label());
                right.add(bar);
                ui.moves.add(new int[]{bar, drain ? 700 : 800});
            }
            if (ui.gen >= 0) right.add(ui.gen);
            if (drain && ui.energy >= 0) ui.energyTo = 0.9f; // an energy upgrade: the gauge goes down slowly
            int slot = b.slot(ItemStack.EMPTY);
            ui.in = new int[ins.size()];
            Arrays.fill(ui.in, -1);
            ui.in[ins.indexOf(upgrade)] = slot;
            ui.consume = false;
            ui.arrow = false;
            b.process(ints(left), new int[]{slot}, false, new int[0], ints(right));
            String type = key(upgrade).getPath().substring("upgrade_".length());
            b.text(Component.translatable("upgrade.mekanism." + type).withStyle(net.minecraft.ChatFormatting.BOLD), upgrade, DARK);
            b.text(Component.translatable("upgrade.mekanism." + type + ".description"));
        } else {
            boolean hasIn = !ins.isEmpty(), hasOut = !outs.isEmpty() || ui.gen >= 0;
            for (Gauge g : gauges) {
                if (g.in()) {
                    int bar = b.bar(0xFF000000 | g.color(), 0.9f, g.label());
                    left.add(bar);
                    ui.moves.add(new int[]{bar, 350});
                    hasIn = true;
                } else {
                    float from = drain ? 0.9f : 0.1f, to = drain ? 0.25f : 0.85f;
                    int bar = b.bar(0xFF000000 | g.color(), from, g.label());
                    right.add(bar);
                    ui.moves.add(new int[]{bar, Math.round(to * 1000)});
                    hasOut = true;
                }
            }
            if (heater) {
                int bar = b.bar(HEAT, 0.05f, Component.translatable("moremekaponder.ui.heat"));
                right.add(bar);
                ui.moves.add(new int[]{bar, 800});
                hasOut = true;
            }
            if (ui.gen >= 0) right.add(ui.gen);
            int n = Math.min(3, ins.size());
            ui.in = new int[ins.size()];
            Arrays.fill(ui.in, -1);
            for (int i = 0; i < n; i++) ui.in[i] = b.slot(ItemStack.EMPTY);
            int m = Math.min(3, outs.size());
            ui.out = new int[m];
            for (int i = 0; i < m; i++) ui.out[i] = i == 0 ? b.bigSlot(ItemStack.EMPTY) : b.slot(ItemStack.EMPTY);
            ui.arrow = hasIn && hasOut;
            int[] shownIn = Arrays.stream(ui.in).filter(v -> v >= 0).toArray();
            b.process(ints(left), shownIn, ui.arrow, ui.out, ints(right));
            List<Component> inNames = new ArrayList<>(), outNames = new ArrayList<>();
            for (Gauge g : gauges) (g.in() ? inNames : outNames).add(g.label());
            if (heater) outNames.add(Component.translatable("moremekaponder.ui.heat"));
            if (ui.gen >= 0) outNames.add(Component.translatable("moremekaponder.ui.generated"));
            if (!ins.isEmpty() || !outs.isEmpty() || (!inNames.isEmpty() && !outNames.isEmpty()))
                b.text(caption(ins, inNames, outs, outNames));
            if (module && ins.size() > 1) b.text(Component.translatable("moremekaponder.ui.module_installed"), ins.get(1), NOTE);
            if (!ins.isEmpty() && !hasOut && !module)
                b.text(Component.translatable("moremekaponder.ui.inserted", ins.get(0).getHoverName()), ItemStack.EMPTY, NOTE);
        }
        b.legend();
        if (o.has("note")) b.text(text(o.get("note")), ItemStack.EMPTY, NOTE);
        ui.link = fx.card(b);
        transient_.add(ui.link);
        return ui;
    }

    /** Gauges only: a tank filling or emptying. */
    private Ui gaugeCard(JsonObject o, String machine, BlockPos at, List<Gauge> gauges, boolean drain) {
        ItemStack icon = machine == null ? ItemStack.EMPTY : stack(machine, 1);
        if (o.has("icon")) icon = stack(o.get("icon").getAsString(), 1);
        Component title = o.has("title") ? text(o.get("title")) : machineName(icon);
        CardBuilder b = CardBuilder.of(title).icon(icon).target(Vec3.atCenterOf(at).add(0, 0.6, 0));
        Ui ui = new Ui();
        ui.arrow = false;
        ui.consume = false;
        List<Integer> bars = new ArrayList<>();
        float from = o.has("from") ? o.get("from").getAsFloat() : drain ? 0.9f : 0.1f;
        float to = o.has("to") ? o.get("to").getAsFloat() : drain ? 0.25f : 0.88f;
        for (Gauge g : gauges) {
            int bar = b.bar(0xFF000000 | g.color(), from, g.label());
            bars.add(bar);
            ui.moves.add(new int[]{bar, Math.round(to * 1000)});
        }
        b.process(new int[0], new int[0], false, new int[0], ints(bars)).legend();
        if (o.has("note")) b.text(text(o.get("note")), ItemStack.EMPTY, NOTE);
        ui.link = fx.card(b);
        transient_.add(ui.link);
        return ui;
    }

    /** A crafting grid with the real recipe (looked up in the game), falling back to the items from the editor. */
    private Ui craftCard(Vec3 top, List<ItemStack> ins, List<ItemStack> outs) {
        ItemStack result = outs.isEmpty() ? ItemStack.EMPTY : outs.get(0);
        ItemStack[] grid = new ItemStack[9];
        Arrays.fill(grid, ItemStack.EMPTY);
        ItemStack shownResult = result.copy();
        boolean found = false;
        var level = Minecraft.getInstance().level;
        if (level != null && !result.isEmpty()) {
            try {
                RecipeHolder<CraftingRecipe> best = null;
                int bestScore = -1;
                for (RecipeHolder<CraftingRecipe> h : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
                    ItemStack r = h.value().getResultItem(level.registryAccess());
                    if (r.isEmpty() || r.getItem() != result.getItem()) continue;
                    int score = 0;
                    for (ItemStack in : ins) {
                        for (Ingredient ing : h.value().getIngredients()) {
                            if (ing.test(in)) { score++; break; }
                        }
                    }
                    if (score > bestScore) { best = h; bestScore = score; }
                }
                if (best != null) {
                    CraftingRecipe r = best.value();
                    shownResult = r.getResultItem(level.registryAccess()).copy();
                    var ings = r.getIngredients();
                    if (r instanceof ShapedRecipe shaped) {
                        int w = shaped.getWidth(), h = shaped.getHeight();
                        for (int y = 0; y < h && y < 3; y++)
                            for (int x = 0; x < w && x < 3; x++) grid[y * 3 + x] = pick(ings.get(y * w + x), result);
                    } else {
                        for (int i = 0; i < ings.size() && i < 9; i++) grid[i] = pick(ings.get(i), result);
                    }
                    found = true;
                }
            } catch (RuntimeException ignored) {
            }
        }
        if (!found) {
            int i = 0;
            for (ItemStack s : ins) for (int k = 0; k < s.getCount() && i < 9; k++) grid[i++] = s.copyWithCount(1);
        }
        ItemStack table = new ItemStack(Items.CRAFTING_TABLE);
        CardBuilder b = CardBuilder.of(table.getHoverName()).icon(table).target(top);
        Ui ui = new Ui();
        int[] cells = new int[9];
        List<Integer> filled = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            cells[i] = grid[i].isEmpty() ? -1 : b.slot(grid[i]);
            if (cells[i] >= 0) filled.add(cells[i]);
        }
        int out = b.bigSlot(ItemStack.EMPTY);
        b.grid(cells, out);
        if (!shownResult.isEmpty()) b.text(Component.translatable("moremekaponder.ui.crafts", count(shownResult)), shownResult, DARK);
        ui.in = new int[0];
        ui.out = new int[]{out};
        ui.consume = false;
        ui.clear.addAll(filled);
        ui.outOverride = List.of(shownResult);
        ui.link = fx.card(b);
        transient_.add(ui.link);
        return ui;
    }

    private static ItemStack pick(Ingredient ing, ItemStack result) {
        ItemStack[] items = ing.getItems();
        if (items.length == 0) return ItemStack.EMPTY;
        String ns = BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace();
        for (ItemStack it : items) {
            if (BuiltInRegistries.ITEM.getKey(it.getItem()).getNamespace().equals(ns)) return it.copyWithCount(1);
        }
        return items[0].copyWithCount(1);
    }

    /** "Iron Ore x3 + Coal -> Iron Dust x4" in the player's language, chemicals included. */
    private static Component caption(List<ItemStack> ins, List<Component> inChems, List<ItemStack> outs, List<Component> outChems) {
        List<Component> left = new ArrayList<>(inChems), right = new ArrayList<>();
        for (ItemStack s : ins) left.add(count(s));
        for (ItemStack s : outs) right.add(count(s));
        right.addAll(outChems);
        MutableComponent c = Component.empty();
        for (int i = 0; i < left.size(); i++) {
            if (i > 0) c.append(" + ");
            c.append(left.get(i));
        }
        if (!right.isEmpty()) {
            c.append(left.isEmpty() ? "→ " : " → ");
            for (int i = 0; i < right.size(); i++) {
                if (i > 0) c.append(" + ");
                c.append(right.get(i));
            }
        }
        return c;
    }

    private static Component caption(List<ItemStack> ins, List<ItemStack> outs, Component product) {
        return caption(ins, List.of(), outs, product == null ? List.of() : List.of(product));
    }

    private static Component count(ItemStack s) {
        MutableComponent c = s.getHoverName().copy();
        if (s.getCount() > 1) c.append(" \u00d7" + s.getCount());
        return c;
    }

    /** A card written in the editor: slots, gauges and rows given one by one. */
    private void card(JsonObject o) {
        String id = o.has("id") ? o.get("id").getAsString() : "card" + cards.size();
        ItemStack icon = o.has("icon") && !o.get("icon").isJsonNull() ? stack(o.get("icon").getAsString(), 1) : ItemStack.EMPTY;
        CardBuilder b = CardBuilder.of(text(o.get("title"))).icon(icon);
        if (o.has("at") && !o.get("at").isJsonNull()) b.target(vec(o.getAsJsonArray("at")));
        java.util.Set<Integer> big = new java.util.HashSet<>();
        if (o.has("big")) for (JsonElement e : o.getAsJsonArray("big")) big.add(e.getAsInt());
        if (o.has("slots")) {
            JsonArray sl = o.getAsJsonArray("slots");
            for (int i = 0; i < sl.size(); i++) {
                ItemStack st = sl.get(i).isJsonNull() ? ItemStack.EMPTY : stackOf(sl.get(i).getAsJsonArray());
                if (big.contains(i)) b.bigSlot(st);
                else b.slot(st);
            }
        }
        if (o.has("bars")) {
            for (JsonElement e : o.getAsJsonArray("bars")) {
                JsonArray a = e.getAsJsonArray();
                b.bar(argb(a.get(0).getAsInt()), a.get(1).getAsFloat(), a.size() > 2 && !a.get(2).isJsonNull() ? text(a.get(2)) : null);
            }
        }
        if (o.has("rows")) {
            for (JsonElement e : o.getAsJsonArray("rows")) {
                JsonArray r = e.getAsJsonArray();
                switch (r.get(0).getAsString()) {
                    case "process" -> b.process(ints(r.get(1)), ints(r.get(2)), r.get(3).getAsBoolean(), ints(r.get(4)), ints(r.get(5)));
                    case "grid" -> b.grid(ints(r.get(1)), r.get(2).getAsInt());
                    case "text" -> b.text(text(r.get(1)),
                            r.size() > 2 && !r.get(2).isJsonNull() ? stack(r.get(2).getAsString(), 1) : ItemStack.EMPTY,
                            r.size() > 3 ? argb(r.get(3).getAsInt()) : DARK);
                    case "legend" -> b.legend();
                    case "icons" -> b.icons(ints(r.get(1)));
                    case "hbar" -> b.hbar(r.get(1).getAsInt(), text(r.get(2)));
                    default -> {
                    }
                }
            }
        }
        ElementLink<CardElement> link = fx.card(b);
        cards.put(id, link);
        if (!(o.has("keep") && o.get("keep").getAsBoolean())) transient_.add(link);
        scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
    }

    /** The tags of an item, read from the game (what the Dictionary shows). */
    private void tagCard(JsonObject o) {
        ItemStack s = stack(o.get("item").getAsString(), 1);
        if (s.isEmpty()) return;
        ItemStack icon = o.has("icon") ? stack(o.get("icon").getAsString(), 1) : ItemStack.EMPTY;
        CardBuilder b = CardBuilder.of(text(o.get("title"))).icon(icon);
        if (o.has("at")) b.target(vec(o.getAsJsonArray("at")));
        b.text(s.getHoverName(), s, DARK);
        List<String> tags = s.getTags().map(t -> "#" + t.location()).sorted().limit(7).toList();
        if (tags.isEmpty()) b.text(Component.translatable("moremekaponder.ui.no_tags"));
        for (String t : tags) b.text(Component.literal(t), ItemStack.EMPTY, 0xFF2A5DB0);
        var link = fx.card(b);
        transient_.add(link);
        scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
    }

    /** Text from the scene data: "@lang.key", ["@lang.key", args...], "#item:mod:id" (the item's name) or plain text. */
    private static Component text(JsonElement e) {
        if (e == null || e.isJsonNull()) return Component.empty();
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            String key = a.get(0).getAsString();
            Object[] args = new Object[a.size() - 1];
            for (int i = 1; i < a.size(); i++) args[i - 1] = text(a.get(i));
            return Component.translatable(key.startsWith("@") ? key.substring(1) : key, args);
        }
        String s = e.getAsString();
        if (s.startsWith("@")) return Component.translatable(s.substring(1));
        if (s.startsWith("#item:")) {
            ItemStack st = stack(s.substring(6), 1);
            return st.isEmpty() ? Component.literal(s.substring(6)) : st.getHoverName();
        }
        return Component.literal(s);
    }

    private static int argb(int c) {
        return (c >>> 24) == 0 ? c | 0xFF000000 : c;
    }

    private static int[] ints(JsonElement e) {
        JsonArray a = e.getAsJsonArray();
        int[] out = new int[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsInt();
        return out;
    }

    private static int[] ints(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

    private static ItemStack stackOf(JsonArray a) {
        return stack(a.get(0).getAsString(), a.size() > 1 ? a.get(1).getAsInt() : 1);
    }

    private static List<ItemStack> stacks(JsonArray a) {
        List<ItemStack> out = new ArrayList<>();
        for (JsonElement e : a) {
            ItemStack s = stackOf(e.getAsJsonArray());
            if (!s.isEmpty()) out.add(s);
        }
        return out;
    }

    private static ResourceLocation key(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem());
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
        if (uiWanted(o)) {
            CardBuilder b = CardBuilder.of(Component.translatable("moremekaponder.ui.equipment"));
            List<Integer> slots = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("equip")) {
                if (e.getAsJsonArray().get(0).getAsString().endsWith("HAND")) continue;
                ItemStack st = stack(e.getAsJsonArray().get(1).getAsString(), 1);
                if (!st.isEmpty()) slots.add(b.slot(st));
            }
            if (!slots.isEmpty()) {
                b.icons(ints(slots));
                transient_.add(fx.card(b));
            }
        }
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

    /** Sets the block's "facing" and any other named properties (string values, as in a blockstate file). */
    private static BlockState props(BlockState st, JsonObject o) {
        if (o.has("facing")) st = setNamed(st, "facing", o.get("facing").getAsString());
        if (o.has("props")) {
            for (var en : o.getAsJsonObject("props").entrySet()) st = setNamed(st, en.getKey(), en.getValue().getAsString());
        }
        return st;
    }

    private static BlockState setNamed(BlockState st, String name, String value) {
        for (Property<?> p : st.getProperties()) {
            if (p.getName().equals(name)) return setProp(st, p, value);
        }
        return st;
    }

    private static <T extends Comparable<T>> BlockState setProp(BlockState st, Property<T> p, String value) {
        return p.getValue(value).map(v -> st.setValue(p, v)).orElse(st);
    }

    /**
     * Items moving along a path (through transporters, into a machine, an arrow in flight): no gravity, set each tick.
     * Either one "item" with "path" (and "count" copies "gap" ticks apart), or "movers": [{item, path, delay}] started together.
     * "trail" leaves particles behind; "mob" moves the scene's mob along with the item (a balloon lifting a chicken).
     */
    private void carry(JsonObject o) {
        record Mover(ItemStack stack, List<Vec3> path, double[] cum, double len, int start, int travel) {
        }
        int speed = o.has("speed") ? o.get("speed").getAsInt() : 6; // ticks per block
        List<Mover> movers = new ArrayList<>();
        java.util.function.BiConsumer<JsonObject, Integer> add = (m, start) -> {
            ItemStack s = stack(m.get("item").getAsString(), 1);
            if (s.isEmpty()) return;
            List<Vec3> path = new ArrayList<>();
            for (JsonElement e : m.getAsJsonArray("path")) path.add(vec(e.getAsJsonArray()));
            if (path.size() < 2) return;
            double[] cum = new double[path.size()];
            for (int i = 1; i < path.size(); i++) cum[i] = cum[i - 1] + path.get(i).distanceTo(path.get(i - 1));
            double len = cum[cum.length - 1];
            movers.add(new Mover(s, path, cum, len, start, Math.max(2, (int) Math.round(len * speed))));
        };
        if (o.has("movers")) {
            for (JsonElement e : o.getAsJsonArray("movers")) {
                JsonObject m = e.getAsJsonObject();
                add.accept(m, m.has("delay") ? m.get("delay").getAsInt() : 0);
            }
        } else {
            int count = o.has("count") ? o.get("count").getAsInt() : 1;
            int gap = o.has("gap") ? o.get("gap").getAsInt() : speed * 2;
            for (int i = 0; i < count; i++) add.accept(o, i * gap);
        }
        if (movers.isEmpty()) return;
        boolean keep = o.has("keep") && o.get("keep").getAsBoolean();
        net.minecraft.core.particles.ParticleOptions trail = null;
        if (o.has("trail")) {
            String t = o.get("trail").getAsString();
            trail = t.equals("flame") ? ParticleTypes.FLAME : t.equals("portal") ? ParticleTypes.PORTAL
                    : t.equals("blue") ? new DustParticleOptions(new Vector3f(0.2f, 0.35f, 0.95f), 1.0f)
                    : new DustParticleOptions(new Vector3f(0.9f, 0.15f, 0.15f), 1.0f);
        }
        Vec3 mobOffset = o.has("mob") && mob != null ? vec(o.getAsJsonArray("mob")) : null;
        List<ElementLink<EntityElement>> links = new ArrayList<>();
        for (int i = 0; i < movers.size(); i++) links.add(null);
        int total = 0;
        for (Mover m : movers) total = Math.max(total, m.start() + m.travel());
        for (int t = 0; t <= total; t++) {
            for (int i = 0; i < movers.size(); i++) {
                Mover m = movers.get(i);
                int lt = t - m.start();
                if (lt == 0) {
                    ElementLink<EntityElement> link = scene.world().createItemEntity(m.path().get(0), Vec3.ZERO, m.stack().copy());
                    scene.world().modifyEntity(link, e -> {
                        e.setNoGravity(true);
                        e.setDeltaMovement(Vec3.ZERO);
                    });
                    links.set(i, link);
                } else if (lt > 0 && lt <= m.travel() && links.get(i) != null) {
                    Vec3 p = along(m.path(), m.cum(), m.len() * lt / m.travel());
                    scene.world().modifyEntity(links.get(i), e -> {
                        e.setPos(p.x, p.y, p.z);
                        e.setDeltaMovement(Vec3.ZERO);
                    });
                    if (trail != null) fx.particleLine(trail, p, p, 1);
                    if (mobOffset != null && i == 0) {
                        Vec3 q = p.add(mobOffset);
                        scene.world().modifyEntity(mob, e -> {
                            e.setNoGravity(true);
                            e.setPos(q.x, q.y, q.z);
                            e.setDeltaMovement(Vec3.ZERO);
                        });
                    }
                    if (lt == m.travel()) {
                        if (keep) drops.add(links.get(i));
                        else scene.world().modifyEntity(links.get(i), Entity::discard);
                    }
                }
            }
            scene.idle(1);
        }
        scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
    }

    private static Vec3 along(List<Vec3> path, double[] cum, double d) {
        for (int i = 1; i < path.size(); i++) {
            if (d <= cum[i] || i == path.size() - 1) {
                double seg = cum[i] - cum[i - 1];
                double f = seg <= 0 ? 1 : Math.min(1, (d - cum[i - 1]) / seg);
                return path.get(i - 1).lerp(path.get(i), f);
            }
        }
        return path.get(path.size() - 1);
    }

    /** Particles running along a path: chemicals and fluids in tubes and pipes, heat in conductors, power in cables. */
    private void stream(JsonObject o) {
        List<Vec3> path = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("path")) path.add(vec(e.getAsJsonArray()));
        net.minecraft.core.particles.ParticleOptions particle;
        String kind = o.has("particle") ? o.get("particle").getAsString() : "dust";
        particle = switch (kind) {
            case "flame" -> ParticleTypes.FLAME;
            case "portal" -> ParticleTypes.PORTAL;
            case "smoke" -> ParticleTypes.SMOKE;
            case "bubble" -> ParticleTypes.BUBBLE_POP;
            case "end_rod" -> ParticleTypes.END_ROD;
            case "crit" -> ParticleTypes.CRIT;
            default -> {
                int col = o.has("color") ? o.get("color").getAsInt() : 0x3CFE9A;
                yield new DustParticleOptions(new Vector3f(((col >> 16) & 255) / 255f, ((col >> 8) & 255) / 255f, (col & 255) / 255f), 1.2f);
            }
        };
        int rounds = o.has("rounds") ? o.get("rounds").getAsInt() : 2;
        int per = o.has("speed") ? o.get("speed").getAsInt() : 3; // ticks per block
        for (int r = 0; r < rounds; r++) {
            for (int i = 1; i < path.size(); i++) {
                Vec3 a = path.get(i - 1), b = path.get(i);
                int n = Math.max(2, (int) Math.round(a.distanceTo(b) * 4));
                fx.particleLine(particle, a, b, n);
                scene.idle(Math.max(1, (int) Math.round(a.distanceTo(b) * per)));
            }
        }
        scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 5);
    }

    /**
     * A player figure for hand-made steps: appears at "at", wears "equip", takes a "pose", follows "path"
     * ([x, y, z, ticks] or [x, y, z, ticks, yaw]), swings "swing" times, or goes away with "hide".
     */
    private void actor(JsonObject o) {
        if (o.has("hide") && o.get("hide").getAsBoolean()) {
            if (player != null) fx.hide(player, 6);
            player = null;
            worn.clear();
            scene.idle(8);
            return;
        }
        float yaw = o.has("yaw") ? o.get("yaw").getAsFloat() : 135;
        Vec3 at = o.has("at") ? vec(o.getAsJsonArray("at")) : (player == null ? new Vec3(0.5, 1, 5.5) : playerAt);
        if (o.has("jump") && player != null) { // instant move (teleport)
            fx.playerMove(player, at, 1);
            playerAt = at;
            scene.idle(2);
        }
        ensurePlayer(at, yaw);
        if (o.has("equip")) wear(o.getAsJsonArray("equip"));
        fx.playerPose(player, o.has("pose") ? PlayerElement.PlayerPose.valueOf(o.get("pose").getAsString()) : PlayerElement.PlayerPose.STAND);
        if (o.has("look")) fx.playerLook(player, o.get("look").getAsFloat());
        if (o.has("path")) {
            for (JsonElement e : o.getAsJsonArray("path")) {
                JsonArray a = e.getAsJsonArray();
                Vec3 p = new Vec3(a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble());
                int ticks = a.get(3).getAsInt();
                if (a.size() > 4) fx.playerTurn(player, a.get(4).getAsFloat(), Math.min(ticks, 6));
                fx.playerMove(player, p, ticks);
                scene.idle(ticks);
                playerAt = p;
            }
        }
        int swings = o.has("swing") ? o.get("swing").getAsInt() : 0;
        for (int i = 0; i < swings; i++) {
            fx.playerSwing(player);
            scene.idle(6);
        }
        scene.idle(o.has("wait") ? o.get("wait").getAsInt() : 10);
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
            default -> { // move to the center (or to "to")
                double dx = (moveTo.x - mobPos.x) / 20, dz = (moveTo.z - mobPos.z) / 20;
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
