package io.github.eeeeeeruki.moremekaponder.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.eeeeeeruki.ponderererer.api.FX;
import io.github.eeeeeeruki.ponderererer.api.PanelBuilder;
import io.github.eeeeeeruki.ponderererer.element.BeamElement;
import io.github.eeeeeeruki.ponderererer.element.GaugeElement;
import io.github.eeeeeeruki.ponderererer.element.PanelElement;
import io.github.eeeeeeruki.ponderererer.element.PlayerElement;
import java.util.ArrayList;
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
import net.minecraft.world.item.Item;
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
    private ElementLink<EntityElement> mob;
    private final List<ElementLink<? extends PonderElement>> transient_ = new ArrayList<>();
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
        if (data.get("main").isJsonNull()) {
            ItemStack hero = stack(data.get("item").getAsString(), 1);
            if (!hero.isEmpty()) scene.world().createItemEntity(new Vec3(1.5, 1.6, 1.5), Vec3.ZERO, hero);
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
        if (!transient_.isEmpty()) scene.idle(8);
        transient_.clear();
    }

    private void op(JsonObject o) {
        switch (o.get("op").getAsString()) {
            case "block" -> {
                BlockPos pos = pos(o.getAsJsonArray("pos"));
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                scene.world().setBlock(pos, state, false);
                scene.world().showSection(util.select().position(pos), Direction.DOWN);
                if (pos.equals(CENTER)) placed = true;
                scene.idle(8);
            }
            case "line" -> {
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                for (JsonElement c : o.getAsJsonArray("cells")) {
                    BlockPos pos = pos(c.getAsJsonArray());
                    scene.world().setBlock(pos, state, false);
                    scene.world().showSection(util.select().position(pos), Direction.DOWN);
                    scene.idle(3);
                }
                scene.idle(6);
            }
            case "swap" -> {
                BlockState state = block(o.get("id").getAsString());
                if (state == null) return;
                if (!placed) {
                    scene.world().setBlock(CENTER, state, false);
                    scene.world().showSection(util.select().position(CENTER), Direction.DOWN);
                    placed = true;
                } else {
                    scene.world().setBlock(CENTER, state, true);
                }
                scene.idle(15);
            }
            case "breakat" -> {
                scene.idle(25);
                scene.world().destroyBlock(pos(o.getAsJsonArray("pos")));
                scene.idle(10);
            }
            case "panel" -> panel(o);
            case "gauge" -> gauges(o.getAsJsonArray("gauges"), o.get("drain").getAsBoolean());
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
            case "click" -> {
                InputElementBuilder in = scene.overlay().showControls(TOP, Pointing.DOWN, 40);
                in = o.get("left").getAsBoolean() ? in.leftClick() : in.rightClick();
                if (o.get("sneak").getAsBoolean()) in = in.whileSneaking();
                if (o.has("item") && !o.get("item").isJsonNull()) {
                    ItemStack held = stack(o.get("item").getAsString(), 1);
                    if (!held.isEmpty()) in.withItem(held);
                }
                scene.idle(45);
            }
            case "mob" -> {
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(rl(o.get("type").getAsString()));
                mob = scene.world().createEntity(level -> {
                    Entity e;
                    try {
                        e = type.create(level);
                    } catch (RuntimeException ex) {
                        e = null;
                    }
                    if (e == null) e = EntityType.PIG.create(level);
                    e.setPos(5.5, 1, 3.5);
                    e.setYRot(90);
                    return e;
                });
                scene.idle(20);
            }
            case "mobact" -> mobAct(o.get("kind").getAsString());
            case "show" -> {
                for (JsonElement id : o.getAsJsonArray("ids")) {
                    ItemStack s = stack(id.getAsString(), 1);
                    if (s.isEmpty()) continue;
                    scene.world().createItemEntity(nextSpot(), Vec3.ZERO, s);
                    scene.idle(4);
                }
                scene.idle(20);
            }
            default -> { // pulse
                if (placed) scene.effects().indicateSuccess(CENTER);
                scene.idle(25);
            }
        }
    }

    private void panel(JsonObject o) {
        Component title = name(o.get("title").isJsonNull() ? null : o.get("title").getAsString());
        PanelBuilder pb = PanelBuilder.machine(title).at(TOP.add(0, 0.6, 0));
        List<Integer> ins = new ArrayList<>(), outs = new ArrayList<>(), bars = new ArrayList<>();
        List<ItemStack> inStacks = new ArrayList<>(), outStacks = new ArrayList<>();
        int x = 26;
        for (JsonElement e : o.getAsJsonArray("in")) {
            if (ins.size() == 3) break;
            JsonArray a = e.getAsJsonArray();
            ItemStack s = stack(a.get(0).getAsString(), a.get(1).getAsInt());
            if (s.isEmpty()) continue;
            ins.add(pb.slot(x, 30, ItemStack.EMPTY));
            inStacks.add(s);
            x += 20;
        }
        pb.arrow(88, 30);
        x = 116;
        for (JsonElement e : o.getAsJsonArray("out")) {
            if (outs.size() == 2) break;
            JsonArray a = e.getAsJsonArray();
            ItemStack s = stack(a.get(0).getAsString(), a.get(1).getAsInt());
            if (s.isEmpty()) continue;
            outs.add(pb.output(x, 30, ItemStack.EMPTY));
            outStacks.add(s);
            x += 22;
        }
        boolean drain = o.has("drain") && o.get("drain").getAsBoolean();
        if (o.has("gauge") && !o.get("gauge").isJsonNull()) {
            int bx = 8;
            for (JsonElement g : o.getAsJsonArray("gauge")) {
                JsonArray a = g.getAsJsonArray();
                String label = a.get(0).getAsString();
                bars.add(pb.bar(bx, 12, 6, 52, 0xFF000000 | a.get(1).getAsInt(), drain ? 1f : 0f,
                        label.isEmpty() ? null : Component.literal(label)));
                bx = 162;
            }
        }
        ElementLink<PanelElement> link = fx.panel(pb);
        transient_.add(link);
        scene.idle(12);
        for (int i = 0; i < ins.size(); i++) {
            fx.panelCursor(link, ins.get(i), true);
            fx.panelSlot(link, ins.get(i), inStacks.get(i));
            scene.idle(10);
        }
        fx.panelCursor(link, -1, false);
        fx.panelProgress(link, 1f, 40);
        for (int b : bars) fx.panelBar(link, b, drain ? 0.3f : 1f, 40);
        scene.idle(42);
        if (!outs.isEmpty()) {
            for (int i : ins) fx.panelSlot(link, i, ItemStack.EMPTY);
            for (int i = 0; i < outs.size(); i++) fx.panelSlot(link, outs.get(i), outStacks.get(i));
        }
        fx.panelProgress(link, 0f, 0);
        scene.idle(25);
    }

    private void gauges(JsonArray gs, boolean drain) {
        int dx = 0;
        for (JsonElement g : gs) {
            JsonArray a = g.getAsJsonArray();
            String label = a.get(0).getAsString();
            ElementLink<GaugeElement> link = fx.gauge(TOP.add(0.6, 0, 0), Component.literal(label),
                    0xFF000000 | a.get(1).getAsInt(), drain ? 100 : 0, 100, "%");
            if (dx != 0) fx.moveGauge(link, dx, 0);
            fx.setGauge(link, drain ? 30 : 100, 40);
            transient_.add(link);
            dx += 34;
        }
        scene.idle(45);
    }

    private void player(JsonObject o) {
        Vec3 feet = new Vec3(1.5, 1, 5.5);
        if (player == null) {
            player = fx.player(feet, 200);
            scene.idle(10);
        }
        for (JsonElement e : o.getAsJsonArray("equip")) {
            JsonArray a = e.getAsJsonArray();
            ItemStack s = stack(a.get(1).getAsString(), 1);
            if (!s.isEmpty()) fx.playerEquip(player, EquipmentSlot.valueOf(a.get(0).getAsString()), s);
        }
        PlayerElement.PlayerPose pose = PlayerElement.PlayerPose.valueOf(o.get("pose").getAsString());
        fx.playerPose(player, pose);
        scene.idle(10);
        if (o.get("move").getAsBoolean()) {
            fx.playerTurn(player, -90, 5);
            fx.playerMove(player, new Vec3(5.5, 1, 5.5), pose == PlayerElement.PlayerPose.SPRINT ? 20 : 40);
            scene.idle(45);
        } else if (o.get("rise").getAsBoolean()) {
            fx.playerMove(player, feet.add(0, 2.5, 0), 40);
            fx.particleLine(ParticleTypes.FLAME, feet.add(0, 0.6, 0), feet.add(0, 2.2, 0), 10);
            scene.idle(45);
        } else if (pose == PlayerElement.PlayerPose.USE_ITEM) {
            fx.playerSwing(player);
            scene.idle(40);
        } else {
            scene.idle(30);
        }
    }

    private void mobAct(String kind) {
        if (mob == null) return;
        ElementLink<EntityElement> m = mob;
        switch (kind) {
            case "kill" -> {
                scene.idle(20);
                scene.world().modifyEntity(m, Entity::discard);
                fx.particleLine(ParticleTypes.POOF, new Vec3(5.5, 1.2, 3.5), new Vec3(5.5, 1.8, 3.5), 8);
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
                for (int i = 0; i < 20; i++) {
                    scene.world().modifyEntity(m, e -> e.setPos(e.getX() - 0.1, e.getY(), e.getZ()));
                    scene.idle(2);
                }
            }
        }
        scene.idle(10);
    }

    private Vec3 nextSpot() {
        Vec3[] spots = {new Vec3(1.5, 1.6, 1.5), new Vec3(5.5, 1.6, 1.5), new Vec3(1.5, 1.6, 5.5), new Vec3(5.5, 1.6, 5.5),
                new Vec3(3.5, 1.6, 0.8), new Vec3(0.8, 1.6, 3.5)};
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
