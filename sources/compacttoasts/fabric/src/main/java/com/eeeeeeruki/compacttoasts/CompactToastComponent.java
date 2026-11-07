package com.eeeeeeruki.compacttoasts;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * バニラの ToastComponent を置き換えるトースト管理。
 * 表示数の上限を設定で変えられるようにし、一定数以上のときは1行ずつのコンパクト表示にする。
 */
public class CompactToastComponent extends ToastComponent {
    private static final long ANIMATION_TIME = 600L;
    static final int ROW_W = 160;
    static final int ROW_H = 16;
    static final int ROW_GAP = 1;
    static final long HEADER_TIME = 2000L;

    private final Minecraft mc;
    private final List<Entry> visibleEntries = new ArrayList<>();
    private final Deque<Toast> queuedToasts = new ArrayDeque<>();
    private long lastFrame = -1L;

    public CompactToastComponent(Minecraft mc) {
        super(mc);
        this.mc = mc;
    }

    @Override
    public void addToast(Toast toast) {
        this.queuedToasts.add(toast);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Toast> T getToast(Class<? extends T> cls, Object token) {
        for (Entry e : this.visibleEntries) {
            if (cls.isAssignableFrom(e.toast.getClass()) && e.toast.getToken().equals(token)) {
                return (T) e.toast;
            }
        }
        for (Toast t : this.queuedToasts) {
            if (cls.isAssignableFrom(t.getClass()) && t.getToken().equals(token)) {
                return (T) t;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        this.visibleEntries.clear();
        this.queuedToasts.clear();
        super.clear();
    }

    /** 置き換え前のバニラの ToastComponent に残っていたトーストを引き継ぐ。 */
    void adopt(ToastComponent old) {
        try {
            for (Field f : ToastComponent.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!Collection.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object value = f.get(old);
                if (!(value instanceof Collection<?> col)) continue;
                for (Object o : col) {
                    Toast t = o instanceof Toast toast ? toast : unwrapInstance(o);
                    if (t != null) this.queuedToasts.add(t);
                }
            }
        } catch (Throwable t) {
            CompactToasts.LOGGER.debug("Could not move old toasts", t);
        }
    }

    @Nullable
    private static Toast unwrapInstance(@Nullable Object instance) throws IllegalAccessException {
        if (instance == null) return null;
        for (Field f : instance.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (Toast.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                return (Toast) f.get(instance);
            }
        }
        return null;
    }

    @Override
    public void render(GuiGraphics g) {
        if (this.mc.options.hideGui) return;

        long now = Util.getMillis();
        long dt = this.lastFrame < 0 ? 0 : Math.min(250L, now - this.lastFrame);
        this.lastFrame = now;

        int screenW = g.guiWidth();
        int screenH = g.guiHeight();
        int max = ToastsConfig.maxToasts();
        int threshold = ToastsConfig.compactThreshold();

        // 待ち行列から表示に入れる
        while (!this.queuedToasts.isEmpty() && this.visibleEntries.size() < max) {
            Toast next = this.queuedToasts.peekFirst();
            if (!this.visibleEntries.isEmpty()) {
                boolean compactAfter = this.visibleEntries.size() + 1 >= threshold;
                if (stackHeight(compactAfter, next) > screenH) break;
            }
            this.queuedToasts.pollFirst();
            this.visibleEntries.add(new Entry(next));
        }

        boolean compact = this.visibleEntries.size() >= threshold;
        float follow = dt <= 0 ? 1.0F : 1.0F - (float) Math.exp(-dt / 70.0);

        int targetY = 0;
        Iterator<Entry> it = this.visibleEntries.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.y < 0) {
                e.y = targetY;
            } else {
                e.y = Mth.lerp(follow, e.y, (float) targetY);
                if (Math.abs(e.y - targetY) < 0.05F) e.y = targetY;
            }
            boolean finished = e.render(g, screenW, compact, now);
            targetY += compact ? ROW_H + ROW_GAP : e.toast.height();
            if (finished) it.remove();
        }
    }

    private int stackHeight(boolean compact, Toast extra) {
        if (compact) return (this.visibleEntries.size() + 1) * (ROW_H + ROW_GAP);
        int h = extra.height();
        for (Entry e : this.visibleEntries) h += e.toast.height();
        return h;
    }

    private final class Entry {
        final Toast toast;
        float y = -1F;
        private long animationTime = -1L;
        private long visibleTime = -1L;
        private Toast.Visibility visibility = Toast.Visibility.SHOW;

        Entry(Toast toast) {
            this.toast = toast;
        }

        private float slide(long now) {
            float f = Mth.clamp((float) (now - this.animationTime) / (float) ANIMATION_TIME, 0.0F, 1.0F);
            f *= f;
            return this.visibility == Toast.Visibility.HIDE ? 1.0F - f : f;
        }

        /** @return true ならこのトーストは表示を終えた */
        boolean render(GuiGraphics g, int screenW, boolean compact, long now) {
            if (this.animationTime == -1L) {
                this.animationTime = now;
                this.visibility.playSound(mc.getSoundManager());
            }
            if (this.visibility == Toast.Visibility.SHOW && now - this.animationTime <= ANIMATION_TIME) {
                this.visibleTime = now;
            }
            float s = slide(now);
            long shown = now - this.visibleTime;
            Toast.Visibility result;

            if (!compact) {
                g.pose().pushPose();
                g.pose().translate((float) screenW - (float) this.toast.width() * s, this.y, 800.0F);
                result = this.toast.render(g, CompactToastComponent.this, shown);
                g.pose().popPose();
            } else {
                ToastInfo info = ToastInfo.of(this.toast);
                if (info != null) {
                    // 表示時間の管理と効果音のため、元のトーストは画面の外で描画する
                    g.pose().pushPose();
                    g.pose().translate(-10000.0F, -10000.0F, 0.0F);
                    result = this.toast.render(g, CompactToastComponent.this, shown);
                    g.pose().popPose();

                    g.pose().pushPose();
                    g.pose().translate((float) screenW - (float) ROW_W * s, this.y, 800.0F);
                    drawRow(g, info, shown);
                    g.pose().popPose();
                } else {
                    // 中身が分からないトーストは縮小して表示する
                    float scale = (float) ROW_H / (float) Math.max(1, this.toast.height());
                    g.pose().pushPose();
                    g.pose().translate((float) screenW - (float) this.toast.width() * scale * s, this.y, 800.0F);
                    g.pose().scale(scale, scale, 1.0F);
                    result = this.toast.render(g, CompactToastComponent.this, shown);
                    g.pose().popPose();
                }
            }

            if (result != this.visibility) {
                this.animationTime = now - (long) ((int) ((1.0F - slide(now)) * (float) ANIMATION_TIME));
                this.visibility = result;
                this.visibility.playSound(mc.getSoundManager());
            }
            return this.visibility == Toast.Visibility.HIDE && now - this.animationTime > ANIMATION_TIME;
        }
    }

    private void drawRow(GuiGraphics g, ToastInfo info, long shown) {
        Font font = this.mc.font;
        g.fill(0, 0, ROW_W, ROW_H, 0xE0101014);
        g.fill(0, 0, 2, ROW_H, info.accent());
        int textX = 6;
        if (info.icon() != null && !info.icon().isEmpty()) {
            g.renderFakeItem(info.icon(), 4, 0);
            textX = 22;
        } else if (info.painter() != null) {
            try {
                info.painter().paint(g, 4, 0);
            } catch (Throwable ignored) {
            }
            textX = 22;
        }
        int maxW = ROW_W - textX - 3;
        int y = (ROW_H - 8) / 2;
        // バニラと同じく、はじめの2秒は「実績達成！」などの見出しを出してから名前に切り替える
        if (info.header() != null && shown < HEADER_TIME && ToastsConfig.headerMode().show(info.advancement())) {
            drawClipped(g, font, info.header(), textX, y, maxW, info.accent() & 0xFFFFFF);
            return;
        }
        Component text = info.text();
        if (info.extra() != null) {
            int mainW = font.width(text);
            int extraW = font.width(info.extra());
            if (mainW + 4 + extraW <= maxW) {
                g.drawString(font, text, textX, y, 0xFFFFFF, true);
                g.drawString(font, info.extra(), textX + mainW + 4, y, 0xA0A0A0, true);
                return;
            }
            if (mainW + 4 < maxW - 20) {
                g.drawString(font, text, textX, y, 0xFFFFFF, true);
                drawClipped(g, font, info.extra(), textX + mainW + 4, y, maxW - mainW - 4, 0xA0A0A0);
                return;
            }
        }
        drawClipped(g, font, text, textX, y, maxW, 0xFFFFFF);
    }

    private static void drawClipped(GuiGraphics g, Font font, Component c, int x, int y, int maxW, int color) {
        if (font.width(c) <= maxW) {
            g.drawString(font, c, x, y, color, true);
            return;
        }
        FormattedText cut = font.substrByWidth(c, Math.max(0, maxW - font.width("...")));
        g.drawString(font, Language.getInstance().getVisualOrder(FormattedText.composite(cut, FormattedText.of("..."))), x, y, color, true);
    }
}
