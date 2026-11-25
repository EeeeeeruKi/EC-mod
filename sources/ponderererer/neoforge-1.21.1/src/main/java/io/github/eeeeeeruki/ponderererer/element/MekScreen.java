package io.github.eeeeeeruki.ponderererer.element;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;

/**
 * A machine screen drawn with the game's own GUI textures (Mekanism's window, slots, gauges, power bars and
 * progress arrows, or a vanilla screen picture), with parts placed at the same spots as in the real screen.
 * Used by {@link CardElement} in screen mode; coordinates are GUI pixels from the window's top-left corner.
 */
public final class MekScreen {
    static ResourceLocation mek(String path) {
        return ResourceLocation.fromNamespaceAndPath("mekanism", "gui/" + path + ".png");
    }

    static final ResourceLocation BASE = mek("base"), INNER = mek("inner_screen"), BAR_BASE = mek("bar/base"),
            POWER_V = mek("bar/vertical_power"), POWER_H = mek("bar/horizontal_power"), OVERLAY_POWER = mek("slot/overlay_power");

    final int width, height;
    int titleX = -1;
    final List<Part> parts = new ArrayList<>();

    MekScreen(int width, int height) {
        this.width = width;
        this.height = height;
    }

    interface Part {
        void draw(CardElement c, GuiGraphics g, Font f, float pt, float a);
    }

    // ------------------------------------------------------------------ parts

    record SlotPart(int x, int y, String kind, int slot) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            int w = 18, h = 18;
            switch (kind) {
                case "output_large" -> { w = 36; h = 54; }
                case "output_wide" -> { w = 42; h = 26; }
                default -> {
                }
            }
            if (!kind.equals("none") && !kind.equals("vanilla")) blit(g, mek("slot/" + kind), x, y, w, h, a);
            if (slot < 0 || slot >= c.slots.size()) return;
            CardElement.Slot s = c.slots.get(slot);
            int ix = x + (w - 16) / 2, iy = y + (h - 16) / 2;
            boolean empty = s.stack == null || s.stack.isEmpty();
            if (kind.equals("power") && empty) blit(g, OVERLAY_POWER, x, y, 18, 18, a);
            if (!empty && a > 0.5f) {
                g.renderItem(s.stack, ix, iy);
                g.renderItemDecorations(f, s.stack, ix, iy);
            }
            if (s.flash > 0) {
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.fill(ix, iy, ix + 16, iy + 16, CardElement.withAlpha(0xFFFFFFFF, a * s.flash / 14f));
                g.pose().popPose();
            }
        }
    }

    record GaugePart(int x, int y, String type, String fill, String frame, int bar) implements Part {
        int[] size() {
            return switch (type) {
                case "small" -> new int[]{18, 30};
                case "small_med" -> new int[]{18, 48};
                case "medium" -> new int[]{34, 60};
                case "wide" -> new int[]{66, 50};
                default -> new int[]{18, 60};
            };
        }

        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            if (type.startsWith("bar") || type.startsWith("hbar")) {
                drawBar(c, g, pt, a);
                return;
            }
            int[] s = size();
            int w = s[0], h = s[1];
            nine(g, mek("gauge/" + frame), x, y, w, h, 1, 5, 5, a);
            if (bar >= 0 && bar < c.bars.size()) {
                CardElement.Bar b = c.bars.get(bar);
                int fh = Math.round((h - 2) * b.get(pt));
                if (fh > 0) {
                    int col = fill.equals("energy") || fill.equals("heat") || fill.equals("lava") ? 0xFFFFFFFF : b.color;
                    tiled(g, ResourceLocation.parse(sprite()), x + 1, y + h - 1 - fh, w - 2, fh, col, a);
                }
            }
            blit(g, mek("gauge/" + type), x + 1, y + 1, w - 2, h - 2, a);
        }

        /** A thin bar (Mekanism's chemical/fluid bar): "bar6x12" fills upward, "hbar116x10" fills to the right. */
        private void drawBar(CardElement c, GuiGraphics g, float pt, float a) {
            boolean horizontal = type.startsWith("hbar");
            String[] wh = type.substring(horizontal ? 4 : 3).split("x");
            int w = Integer.parseInt(wh[0]), h = Integer.parseInt(wh[1]);
            nine(g, BAR_BASE, x, y, w, h, 1, 5, 5, a);
            if (bar < 0 || bar >= c.bars.size()) return;
            CardElement.Bar b = c.bars.get(bar);
            float v = b.get(pt);
            String sprite = sprite();
            int col = fill.equals("energy") || fill.equals("heat") || fill.equals("lava") ? 0xFFFFFFFF : b.color;
            if (horizontal) {
                int fw = Math.round((w - 2) * v);
                if (fw > 0) tiled(g, ResourceLocation.parse(sprite), x + 1, y + 1, fw, h - 2, col, a);
            } else {
                int fh = Math.round((h - 2) * v);
                if (fh > 0) tiled(g, ResourceLocation.parse(sprite), x + 1, y + h - 1 - fh, w - 2, fh, col, a);
            }
        }

        private String sprite() {
            return switch (fill) {
                case "fluid" -> "minecraft:block/water_still";
                case "lava" -> "minecraft:block/lava_still";
                case "energy" -> "mekanism:liquid/energy";
                case "heat" -> "mekanism:liquid/heat";
                default -> "mekanism:liquid/liquid";
            };
        }
    }

    record PowerPart(int x, int y, boolean vertical, int bar) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            float v = bar >= 0 && bar < c.bars.size() ? c.bars.get(bar).get(pt) : 0f;
            setColor(g, 1, a);
            if (vertical) {
                nine(g, BAR_BASE, x, y, 6, 54, 1, 5, 5, a);
                int fh = Math.round(52 * v);
                if (fh > 0) g.blit(POWER_V, x + 1, y + 1 + 52 - fh, 4, fh, 0, 52 - fh, 4, fh, 4, 52);
            } else {
                nine(g, BAR_BASE, x, y, 54, 6, 1, 5, 5, a);
                int fw = Math.round(52 * v);
                if (fw > 0) g.blit(POWER_H, x + 1, y + 1, fw, 4, 0, 0, fw, 4, 52, 4);
            }
            setColor(g, 1, 1);
        }
    }

    record ProgressPart(int x, int y, String type) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            int w, h;
            String file;
            switch (type) {
                case "bar" -> { w = 25; h = 9; file = "bar"; }
                case "large_right", "large_left" -> { w = 48; h = 8; file = type; }
                case "small_right", "small_left" -> { w = 28; h = 8; file = type; }
                case "tall_right" -> { w = 22; h = 15; file = type; }
                case "bi" -> { w = 16; h = 6; file = "bidirectional"; }
                default -> { w = 32; h = 8; file = "right"; }
            }
            ResourceLocation tex = mek("progress/" + file);
            setColor(g, 1, a);
            g.blit(tex, x, y, 0, 0, w, h, w, h * 3);
            int fw = Math.round(w * c.progress.get(pt));
            if (fw > 0) {
                if (type.endsWith("left")) g.blit(tex, x + w - fw, y, w - fw, h, fw, h, w, h * 3);
                else g.blit(tex, x, y, 0, h, fw, h, w, h * 3);
            }
            setColor(g, 1, 1);
        }
    }

    record InnerScreenPart(int x, int y, int w, int h) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            nine(g, INNER, x, y, w, h, 4, 256, 256, a);
        }
    }

    record TextPart(int x, int y, int text, int color, int maxWidth, boolean centered) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            if (text < 0 || text >= c.texts.size()) return;
            List<FormattedCharSequence> lines = f.split(c.texts.get(text), Math.max(20, maxWidth));
            int ty = y;
            for (FormattedCharSequence line : lines) {
                int tx = centered ? x - f.width(line) / 2 : x;
                g.drawString(f, line, tx, ty, CardElement.withAlpha(color, a), false);
                ty += 9;
            }
        }
    }

    record ImagePart(ResourceLocation tex, int x, int y, int u, int v, int w, int h, int texW, int texH) implements Part {
        @Override
        public void draw(CardElement c, GuiGraphics g, Font f, float pt, float a) {
            setColor(g, 1, a);
            g.blit(tex, x, y, u, v, w, h, texW, texH);
            setColor(g, 1, 1);
        }
    }

    // ------------------------------------------------------------------ drawing helpers

    static void setColor(GuiGraphics g, float rgb, float a) {
        g.setColor(rgb, rgb, rgb, a);
    }

    static void blit(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, float a) {
        setColor(g, 1, a);
        RenderSystem.enableBlend();
        g.blit(tex, x, y, 0, 0, w, h, w, h);
        setColor(g, 1, 1);
    }

    /** A texture stretched like a window: the corners stay, the sides and middle stretch. */
    static void nine(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int b, int tw, int th, float a) {
        setColor(g, 1, a);
        RenderSystem.enableBlend();
        int cw = tw - 2 * b, ch = th - 2 * b, iw = w - 2 * b, ih = h - 2 * b;
        g.blit(tex, x, y, 0, 0, b, b, tw, th);
        g.blit(tex, x + w - b, y, tw - b, 0, b, b, tw, th);
        g.blit(tex, x, y + h - b, 0, th - b, b, b, tw, th);
        g.blit(tex, x + w - b, y + h - b, tw - b, th - b, b, b, tw, th);
        if (iw > 0) {
            g.blit(tex, x + b, y, iw, b, b, 0, cw, b, tw, th);
            g.blit(tex, x + b, y + h - b, iw, b, b, th - b, cw, b, tw, th);
        }
        if (ih > 0) {
            g.blit(tex, x, y + b, b, ih, 0, b, b, ch, tw, th);
            g.blit(tex, x + w - b, y + b, b, ih, tw - b, b, b, ch, tw, th);
        }
        if (iw > 0 && ih > 0) g.blit(tex, x + b, y + b, iw, ih, b, b, cw, ch, tw, th);
        setColor(g, 1, 1);
    }

    /** A block-atlas texture (a chemical or fluid) tiled over an area, tinted. */
    static void tiled(GuiGraphics g, ResourceLocation spriteId, int x, int y, int w, int h, int argb, float a) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(spriteId);
        float r = ((argb >> 16) & 255) / 255f, gr = ((argb >> 8) & 255) / 255f, bl = (argb & 255) / 255f;
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        Matrix4f m = g.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int ty = 0; ty < h; ty += 16) {
            int th = Math.min(16, h - ty);
            int y1 = y + h - ty, y0 = y1 - th;
            for (int tx = 0; tx < w; tx += 16) {
                int tw = Math.min(16, w - tx);
                float u0 = sprite.getU0(), u1 = sprite.getU(tw / 16f);
                float v1 = sprite.getV1(), v0 = sprite.getV(1f - th / 16f);
                int x0 = x + tx, x1 = x0 + tw;
                bb.addVertex(m, x0, y1, 0).setUv(u0, v1).setColor(r, gr, bl, a);
                bb.addVertex(m, x1, y1, 0).setUv(u1, v1).setColor(r, gr, bl, a);
                bb.addVertex(m, x1, y0, 0).setUv(u1, v0).setColor(r, gr, bl, a);
                bb.addVertex(m, x0, y0, 0).setUv(u0, v0).setColor(r, gr, bl, a);
            }
        }
        var mesh = bb.build();
        if (mesh != null) BufferUploader.drawWithShader(mesh);
    }
}
