package io.github.eeeeeeruki.hyperponder;

import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.Vec3;

public class HyperScenes {

    private static ItemStack stack(ResourceLocation item) {
        return new ItemStack(BuiltInRegistries.ITEM.get(item));
    }

    private static ElementLink<EntityElement> spawn(SceneBuilder scene, SceneBuildingUtil util, EntityType<? extends Mob> type, int x, int z) {
        return scene.world().createEntity(level -> {
            Mob mob = type.create(level);
            Vec3 p = util.vector().topOf(x, 0, z);
            mob.setPos(p.x, p.y, p.z);
            mob.xo = p.x;
            mob.yo = p.y;
            mob.zo = p.z;
            mob.setNoAi(true);
            return mob;
        });
    }

    private static void poof(SceneBuilder scene, SceneBuildingUtil util, int x, int z) {
        scene.effects().emitParticles(util.vector().centerOf(x, 1, z),
            scene.effects().simpleParticleEmitter(ParticleTypes.POOF, Vec3.ZERO), 12, 2);
    }

    public static void muteki(SceneBuilder scene, SceneBuildingUtil util) {
        ResourceLocation item = new ResourceLocation("hyperdaimc", "muteki");
        scene.title("muteki", "Muteki Star");

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(10);
        Vec3 center = util.vector().topOf(2, 0, 2);
        scene.overlay().showControls(center.add(0, 1.2, 0), Pointing.DOWN, 60).withItem(stack(item));
        scene.overlay().showText(70).text("Keep the Muteki Star in your hotbar and you become Muteki.").pointAt(center.add(0, 1.2, 0)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("Command selectors such as @e can no longer pick you.").pointAt(center).placeNearTarget().colored(PonderPalette.BLUE).attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("Your items cannot be stolen either.").pointAt(center).placeNearTarget().colored(PonderPalette.GREEN).attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("It also ignores the Novel Calibur's instant kill.").pointAt(center).placeNearTarget().colored(PonderPalette.RED).attachKeyFrame();
        scene.idle(80);
    }

    public static void novel(SceneBuilder scene, SceneBuildingUtil util) {
        ResourceLocation item = new ResourceLocation("hyperdaimc", "novel");
        scene.title("novel", "Novel Calibur");

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(10);
        spawn(scene, util, EntityType.PIG, 1, 3);
        spawn(scene, util, EntityType.COW, 3, 1);
        ElementLink<EntityElement> sheep = spawn(scene, util, EntityType.SHEEP, 3, 3);
        scene.idle(15);
        Vec3 center = util.vector().topOf(2, 0, 2);
        scene.overlay().showControls(center.add(0, 1.5, 0), Pointing.RIGHT, 60).leftClick().withItem(stack(item));
        scene.overlay().showText(70).text("Left-click and every entity near your line of sight is defeated.").pointAt(center).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.world().modifyEntities(Mob.class, e -> e.discard());
        poof(scene, util, 1, 3);
        poof(scene, util, 3, 1);
        poof(scene, util, 3, 3);
        scene.idle(30);
        spawn(scene, util, EntityType.PIG, 1, 3);
        spawn(scene, util, EntityType.COW, 3, 1);
        spawn(scene, util, EntityType.SHEEP, 3, 3);
        scene.idle(15);
        scene.overlay().showControls(center.add(0, 1.5, 0), Pointing.RIGHT, 60).leftClick().whileSneaking().withItem(stack(item));
        scene.overlay().showText(70).text("While sneaking, only the single entity you are looking at is defeated.").pointAt(util.vector().topOf(3, 0, 1)).placeNearTarget().colored(PonderPalette.BLUE).attachKeyFrame();
        scene.idle(80);
        scene.world().modifyEntities(Cow.class, e -> e.discard());
        poof(scene, util, 3, 1);
        scene.idle(30);
        scene.overlay().showText(80).text("Rare drops, experience orbs and enchantment effects still work.").independent(40).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80).text("It can also be used as a modifier in Tinkers' Construct.").independent(40);
        scene.idle(90);
    }

    public static void chronicle(SceneBuilder scene, SceneBuildingUtil util) {
        ResourceLocation item = new ResourceLocation("hyperdaimc", "chronicle");
        scene.title("chronicle", "Chronicle Clock");

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(10);
        BlockPos start = new BlockPos(0, 1, 0);
        BlockPos end = new BlockPos(4, 1, 4);
        scene.overlay().showControls(util.vector().blockSurface(start, Direction.UP), Pointing.DOWN, 40).rightClick().withItem(stack(item));
        scene.overlay().showText(60).text("Right-click a start point...").pointAt(util.vector().blockSurface(start, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(70);
        scene.overlay().showControls(util.vector().blockSurface(end, Direction.UP), Pointing.DOWN, 40).rightClick().withItem(stack(item));
        scene.overlay().showText(60).text("...and then an end point.").pointAt(util.vector().blockSurface(end, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(70);
        Selection area = util.select().fromTo(0, 1, 0, 4, 3, 4);
        scene.overlay().showOutlineWithText(area, 100).colored(PonderPalette.GREEN).text("The region between them becomes protected.").placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80).text("Inside, players can only touch things as if they were in Adventure mode.").independent(30).colored(PonderPalette.RED).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80).text("By default only the owner can act inside. A paused region blocks even the owner.").independent(30).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showControls(util.vector().centerOf(2, 1, 2), Pointing.DOWN, 40).leftClick().withItem(stack(item));
        scene.overlay().showText(70).text("Left-click the region to release the protection.").pointAt(util.vector().centerOf(2, 1, 2)).placeNearTarget().colored(PonderPalette.BLUE).attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("It overrides the Pickdox.").independent(30);
        scene.idle(80);
    }

    public static void paradox(SceneBuilder scene, SceneBuildingUtil util) {
        ResourceLocation item = new ResourceLocation("hyperdaimc", "paradox");
        scene.title("paradox", "Pickdox");

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(10);
        BlockPos target = new BlockPos(2, 1, 2);
        scene.overlay().showControls(util.vector().blockSurface(target, Direction.UP), Pointing.DOWN, 50).leftClick().withItem(stack(item));
        scene.overlay().showText(70).text("Left-click a block to mine it instantly.").pointAt(util.vector().blockSurface(target, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(60);
        scene.world().destroyBlock(target);
        scene.idle(30);
        BlockPos a = new BlockPos(0, 1, 0);
        BlockPos b = new BlockPos(4, 3, 4);
        scene.overlay().showControls(util.vector().blockSurface(a, Direction.UP), Pointing.DOWN, 40).rightClick().withItem(stack(item));
        scene.overlay().showText(70).text("Right-click a start and an end point to create a combo chain.").pointAt(util.vector().blockSurface(a, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showOutlineWithText(util.select().fromTo(0, 1, 0, 4, 3, 4), 90).colored(PonderPalette.GREEN).text("Mining a block inside the chain mines the whole chain at once.").placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.overlay().showControls(util.vector().blockSurface(new BlockPos(1, 1, 1), Direction.UP), Pointing.DOWN, 40).leftClick().withItem(stack(item));
        scene.idle(30);
        for (int y = 3; y >= 1; y--) {
            for (int x = 0; x < 5; x++) {
                for (int z = 0; z < 5; z++) {
                    scene.world().destroyBlock(new BlockPos(x, y, z));
                }
            }
            scene.idle(8);
        }
        scene.idle(20);
        scene.overlay().showText(80).text("Middle-click a chain to make a combo cluster. Sneak and right-click to dissolve it.").independent(30).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80).text("Drops go straight into your inventory; otherwise they are stored inside the item.").independent(30).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80).text("Throw the item to deliver its contents to the storage you are looking at.").independent(30);
        scene.idle(90);
    }

    public static void vrx(SceneBuilder scene, SceneBuildingUtil util) {
        ResourceLocation item = new ResourceLocation("hyperdaimc", "vrx");
        scene.title("vrx", "V.R.X.");

        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().layersFrom(1), Direction.DOWN);
        scene.idle(10);
        BlockPos target = new BlockPos(2, 1, 2);
        Vec3 face = util.vector().blockSurface(target, Direction.UP);
        scene.overlay().showControls(face, Pointing.DOWN, 50).rightClick().withItem(stack(item));
        scene.overlay().showText(70).text("Right-click a block or an entity to open its VRX.").pointAt(face).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.overlay().showText(70).text("Set the contents in the GUI and close it to give the VRX to the target.").pointAt(face).placeNearTarget().colored(PonderPalette.GREEN).attachKeyFrame();
        scene.idle(80);
        scene.effects().indicateSuccess(target);
        scene.idle(30);
        scene.overlay().showControls(face, Pointing.DOWN, 50).leftClick().withItem(stack(item));
        scene.overlay().showText(70).text("Left-click a block or an entity to dissolve its VRX.").pointAt(face).placeNearTarget().colored(PonderPalette.RED).attachKeyFrame();
        scene.idle(80);
        scene.effects().indicateRedstone(target);
        scene.idle(30);
        scene.overlay().showText(80).text("Right-click the player inside the GUI to open your own VRX.").independent(30).attachKeyFrame();
        scene.idle(90);
        scene.overlay().showText(80).text("The contents can also be set from JEI or EMI when they are installed.").independent(30);
        scene.idle(90);
    }


}
