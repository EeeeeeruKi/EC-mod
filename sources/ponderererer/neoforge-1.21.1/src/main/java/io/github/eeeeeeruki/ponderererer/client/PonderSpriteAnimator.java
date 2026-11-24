package io.github.eeeeeeruki.ponderererer.client;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Keeps animated block textures moving inside Ponder scenes when Sodium or Embeddium is installed.
 * Those mods only animate textures they saw in the world this frame, and Ponder draws its scenes itself, so
 * animated machine faces (a working crusher, lava, water...) froze. Each frame the Ponder screen is drawn, the
 * textures of the blocks in the scene are marked as visible.
 */
public final class PonderSpriteAnimator {
    private static final Map<BlockState, List<TextureAtlasSprite>> SPRITES = new ConcurrentHashMap<>();
    private static final Direction[] SIDES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static Consumer<TextureAtlasSprite> marker;
    private static boolean looked;

    private PonderSpriteAnimator() {
    }

    public static void onOpen(ScreenEvent.Opening event) {
        if (event.getNewScreen() instanceof PonderUI) SPRITES.clear(); // models may have been reloaded
    }

    public static void onRender(ScreenEvent.Render.Pre event) {
        if (!(event.getScreen() instanceof PonderUI ui)) return;
        Consumer<TextureAtlasSprite> mark = marker();
        if (mark == null) return;
        try {
            PonderScene scene = ui.getActiveScene();
            if (scene == null) return;
            Set<BlockState> seen = new HashSet<>();
            for (BlockState state : scene.getWorld().getBlockMap().values()) {
                if (!seen.add(state)) continue;
                for (TextureAtlasSprite sprite : sprites(state)) mark.accept(sprite);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private static List<TextureAtlasSprite> sprites(BlockState state) {
        return SPRITES.computeIfAbsent(state, s -> {
            Set<TextureAtlasSprite> out = new LinkedHashSet<>();
            try {
                BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(s);
                RandomSource rand = RandomSource.create(42L);
                for (Direction side : SIDES) {
                    rand.setSeed(42L);
                    for (BakedQuad quad : model.getQuads(s, side, rand, ModelData.EMPTY, null)) out.add(quad.getSprite());
                }
                out.add(model.getParticleIcon(ModelData.EMPTY));
            } catch (RuntimeException ignored) {
            }
            return new ArrayList<>(out);
        });
    }

    /** Sodium's or Embeddium's "this texture is on screen" call, or null when neither is installed. */
    private static Consumer<TextureAtlasSprite> marker() {
        if (looked) return marker;
        looked = true;
        String[] classes = {"net.caffeinemc.mods.sodium.api.texture.SpriteUtil", "org.embeddedt.embeddium.api.render.texture.SpriteUtil",
                "me.jellysquid.mods.sodium.client.render.texture.SpriteUtil"};
        for (String name : classes) {
            try {
                Class<?> c = Class.forName(name);
                Method m = c.getMethod("markSpriteActive", TextureAtlasSprite.class);
                Object target = null;
                if (!Modifier.isStatic(m.getModifiers())) {
                    Field f = c.getField("INSTANCE");
                    target = f.get(null);
                    if (target == null) continue;
                }
                Object t = target;
                marker = sprite -> {
                    try {
                        m.invoke(t, sprite);
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                    }
                };
                return marker;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            }
        }
        return null;
    }
}
