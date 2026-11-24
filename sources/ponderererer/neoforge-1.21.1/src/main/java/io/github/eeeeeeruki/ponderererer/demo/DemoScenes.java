package io.github.eeeeeeruki.ponderererer.demo;

import io.github.eeeeeeruki.ponderererer.api.FX;
import io.github.eeeeeeruki.ponderererer.api.PanelBuilder;
import io.github.eeeeeeruki.ponderererer.api.CardBuilder;
import io.github.eeeeeeruki.ponderererer.element.CardElement;
import io.github.eeeeeeruki.ponderererer.element.LabelElement;
import io.github.eeeeeeruki.ponderererer.element.BeamElement;
import io.github.eeeeeeruki.ponderererer.element.GaugeElement;
import io.github.eeeeeeruki.ponderererer.element.LightningElement;
import io.github.eeeeeeruki.ponderererer.element.PanelElement;
import io.github.eeeeeeruki.ponderererer.element.PlayerElement;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Demo scenes that show every Ponderererer effect. */
public final class DemoScenes {
    private DemoScenes() {}

    public static void beamsAndArcs(SceneBuilder scene, SceneBuildingUtil util) {
        FX fx = FX.of(scene);
        scene.title("ponderererer_demo_beams", "Ponderererer: Lasers, Lightning and Explosions");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        BlockPos src = new BlockPos(1, 1, 3), target = new BlockPos(5, 1, 3);
        scene.world().setBlock(src, Blocks.OBSERVER.defaultBlockState(), false);
        scene.world().setBlock(target, Blocks.STONE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(src), Direction.DOWN);
        scene.world().showSection(util.select().position(target), Direction.DOWN);
        scene.idle(15);

        Vec3 from = util.vector().centerOf(src).add(0.5, 0, 0);
        Vec3 to = util.vector().centerOf(target).add(-0.5, 0, 0);
        ElementLink<BeamElement> laser = fx.laserShoot(from, to, 0xFFFF3030, 0.07f, 10);
        fx.sound(SoundEvents.BEACON_ACTIVATE, 0.6f, 1.6f);
        scene.overlay().showText(60).text("A laser beam drawn with FX.laser").pointAt(util.vector().centerOf(3, 1, 3)).placeNearTarget();
        scene.idle(70);
        fx.hide(laser, 8);
        scene.idle(15);

        ElementLink<LightningElement> arc = fx.lightning(util.vector().topOf(src), util.vector().topOf(target), 0xFF80C8FF);
        fx.sound(SoundEvents.TRIDENT_THUNDER, 0.3f, 1.8f);
        scene.overlay().showText(60).text("An electric arc drawn with FX.lightning").pointAt(util.vector().topOf(3, 1, 3)).placeNearTarget();
        scene.idle(70);
        fx.hide(arc, 6);
        scene.idle(15);

        fx.explosion(util.vector().centerOf(target), 1.5f, true);
        scene.overlay().showText(50).text("FX.explosion: particles, flash, sound and broken blocks").pointAt(util.vector().centerOf(target)).placeNearTarget();
        scene.idle(60);
        scene.markAsFinished();
    }

    public static void gaugesAndPanels(SceneBuilder scene, SceneBuildingUtil util) {
        FX fx = FX.of(scene);
        scene.title("ponderererer_demo_gauges", "Ponderererer: Gauges and Machine Screens");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        BlockPos furnace = new BlockPos(3, 1, 3);
        scene.world().setBlock(furnace, Blocks.BLAST_FURNACE.defaultBlockState(), false);
        scene.world().showSection(util.select().position(furnace), Direction.DOWN);
        scene.idle(15);

        ElementLink<GaugeElement> energy = fx.gauge(util.vector().topOf(furnace), Component.literal("Energy"), 0xFF3CFE9A, 0, 10000, "FE");
        fx.setGauge(energy, 10000, 40);
        scene.overlay().showText(50).text("FX.gauge shows an animated bar next to a block").pointAt(util.vector().blockSurface(furnace, Direction.WEST)).placeNearTarget();
        scene.idle(60);
        fx.hide(energy, 6);
        scene.idle(10);

        PanelBuilder pb = PanelBuilder.machine(Component.literal("Machine"));
        int in = pb.slot(56, 30, ItemStack.EMPTY);
        int out = pb.output(116, 30, ItemStack.EMPTY);
        pb.arrow(80, 30);
        int bar = pb.bar(160, 12, 6, 52, 0xFF3CFE9A, 1f, null);
        ElementLink<PanelElement> panel = fx.panel(pb);
        scene.idle(10);
        fx.panelCursor(panel, in, true);
        fx.panelSlot(panel, in, new ItemStack(Items.RAW_IRON, 3));
        scene.overlay().showText(50).text("FX.panel draws a fake machine window with slots").independent(10);
        scene.idle(30);
        fx.panelCursor(panel, -1, false);
        fx.panelProgress(panel, 1f, 40);
        fx.panelBar(panel, bar, 0.6f, 40);
        scene.idle(40);
        fx.panelSlot(panel, in, ItemStack.EMPTY);
        fx.panelSlot(panel, out, new ItemStack(Items.IRON_INGOT, 4));
        fx.panelProgress(panel, 0f, 0);
        scene.idle(40);
        fx.hide(panel, 8);
        scene.idle(10);

        var night = fx.tint(0x90000010, 20);
        scene.overlay().showText(40).text("FX.tint darkens the view, e.g. for night").independent(40);
        scene.idle(50);
        fx.hide(night, 20);
        scene.idle(25);
        scene.markAsFinished();
    }

    public static void players(SceneBuilder scene, SceneBuildingUtil util) {
        FX fx = FX.of(scene);
        scene.title("ponderererer_demo_players", "Ponderererer: Player Figures");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        scene.idle(10);

        ElementLink<PlayerElement> p = fx.player(new Vec3(1.5, 1, 3.5), -90);
        fx.playerEquip(p, EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_PICKAXE));
        scene.overlay().showText(40).text("FX.player places a posable player").pointAt(new Vec3(1.5, 2.8, 3.5)).placeNearTarget();
        scene.idle(45);

        fx.playerPose(p, PlayerElement.PlayerPose.WALK);
        fx.playerMove(p, new Vec3(4.5, 1, 3.5), 40);
        scene.idle(40);
        fx.playerPose(p, PlayerElement.PlayerPose.STAND);
        fx.playerSwing(p);
        scene.idle(15);
        fx.playerSwing(p);
        scene.idle(20);

        fx.playerEquip(p, EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        fx.playerPose(p, PlayerElement.PlayerPose.FLY);
        fx.playerMove(p, new Vec3(4.5, 3, 3.5), 30);
        scene.overlay().showText(50).text("It can wear armor (mod armor too) and move around").pointAt(new Vec3(4.5, 4.5, 3.5)).placeNearTarget();
        scene.idle(40);
        fx.playerPose(p, PlayerElement.PlayerPose.ELYTRA);
        fx.playerTurn(p, 90, 10);
        fx.playerMove(p, new Vec3(1.5, 2, 3.5), 40);
        scene.idle(45);
        fx.playerPose(p, PlayerElement.PlayerPose.SNEAK);
        fx.playerMove(p, new Vec3(1.5, 1, 3.5), 10);
        scene.idle(30);
        scene.markAsFinished();
    }

    public static void cardsAndLight(SceneBuilder scene, SceneBuildingUtil util) {
        FX fx = FX.of(scene);
        scene.title("ponderererer_demo_cards", "Ponderererer: Cards, Labels and Light");
        scene.configureBasePlate(0, 0, 7);
        scene.showBasePlate();
        BlockPos furnace = new BlockPos(3, 1, 3);
        scene.world().setBlock(furnace, Blocks.BLAST_FURNACE.defaultBlockState(), false);
        scene.world().showSection(util.select().fromTo(0, 1, 0, 6, 3, 6), Direction.DOWN);
        scene.idle(15);

        CardBuilder cb = CardBuilder.of(Component.translatable("block.minecraft.blast_furnace"))
                .icon(new ItemStack(Items.BLAST_FURNACE)).target(util.vector().topOf(furnace));
        int fuel = cb.bar(0xFFFF8030, 1f, Component.literal("Fuel"));
        int in = cb.slot(ItemStack.EMPTY), out = cb.bigSlot(ItemStack.EMPTY);
        cb.process(new int[]{fuel}, new int[]{in}, true, new int[]{out}, new int[0]).legend();
        cb.text(Component.literal("Raw Iron x3 -> Iron Ingot x3"));
        ElementLink<CardElement> card = fx.card(cb);
        scene.overlay().showText(60).text("FX.card shows a machine-style card on the left, linked to its block").independent(10);
        scene.idle(20);
        fx.cardSlot(card, in, new ItemStack(Items.RAW_IRON, 3));
        scene.idle(10);
        fx.cardProgress(card, 1f, 40);
        fx.cardBar(card, fuel, 0.5f, 40);
        scene.idle(40);
        fx.cardSlot(card, in, ItemStack.EMPTY);
        fx.cardSlot(card, out, new ItemStack(Items.IRON_INGOT, 3));
        fx.cardProgress(card, 0f, 0);
        ElementLink<LabelElement> label = fx.label(util.vector().topOf(furnace), Component.literal("Output"), 0xFF5080FF);
        scene.overlay().showText(50).text("FX.label puts a name tag above a point").pointAt(util.vector().topOf(furnace)).placeNearTarget();
        scene.idle(60);
        fx.hide(card, 8);
        fx.hide(label, 8);
        scene.idle(10);

        BlockPos lamp = new BlockPos(1, 1, 1);
        scene.world().setBlock(lamp, Blocks.GLOWSTONE.defaultBlockState(), false);
        scene.idle(10);
        fx.darkness(0);
        fx.lightSource(lamp, 15);
        scene.overlay().showText(60).text("FX.darkness and FX.lightSource light the scene like the real world").independent(10);
        scene.idle(70);
        fx.clearLight();
        scene.idle(20);
        scene.markAsFinished();
    }
}
