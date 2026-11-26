package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.course.RaceManager;
import com.eeeeeeruki.skyriders.net.RaceStatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

/** レース中の表示（画面の上）：コース・ラップ・タイム・次のチェックポイント */
public final class RaceHud {
    private RaceHud() {
    }

    private static RaceStatePayload state;
    private static int ticks; // 受け取ってから数えたtick

    public static void receive(RaceStatePayload payload) {
        state = payload.active() ? payload : null;
        ticks = payload.elapsed();
    }

    public static void tick() {
        if (state != null) ticks++;
    }

    public static void render(DrawContext ctx, RenderTickCounter counter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (state == null || mc.player == null || mc.options.hudHidden) return;
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth();
        Text line1 = Text.translatable("hud.skyriders.race_line1", state.course(), state.lap(), state.laps());
        Text time = Text.literal(RaceManager.formatTime(ticks));
        Text line2 = state.next() <= state.total()
                ? Text.translatable("hud.skyriders.race_next", state.next(), state.total())
                : Text.translatable("hud.skyriders.race_to_goal");
        int boxW = Math.max(tr.getWidth(line1), Math.max(tr.getWidth(line2), tr.getWidth(time) * 2)) + 16;
        ctx.fill(w / 2 - boxW / 2, 4, w / 2 + boxW / 2, 52, 0x80000000);
        ctx.drawCenteredTextWithShadow(tr, line1, w / 2, 8, 0xFFE070);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(w / 2f, 19f, 0f);
        ctx.getMatrices().scale(2f, 2f, 1f);
        ctx.drawCenteredTextWithShadow(tr, time, 0, 0, 0xFFFFFF);
        ctx.getMatrices().pop();
        ctx.drawCenteredTextWithShadow(tr, line2, w / 2, 40, state.next() <= state.total() ? 0x80C0FF : 0x80FF80);
    }
}
