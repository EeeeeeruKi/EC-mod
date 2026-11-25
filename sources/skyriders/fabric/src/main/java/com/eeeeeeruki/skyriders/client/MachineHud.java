package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** チャージ・スペシャル・速度・ほおばり中の表示 */
public final class MachineHud {
    private MachineHud() {
    }

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (!(client.player.getVehicle() instanceof MachineEntity m)) return;

        TextRenderer tr = client.textRenderer;
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int barW = 120;
        int x = w / 2 - barW / 2;
        int y = h - 78;

        // 速度
        int kmh = Math.round(m.getSpeed() * 20f * 3.6f);
        Text speedText = Text.translatable("hud.skyriders.speed", kmh);
        ctx.drawTextWithShadow(tr, speedText, w / 2 - tr.getWidth(speedText) / 2, y - 34, 0xFFFFFF);

        // チャージゲージ
        float charge = m.getCharge();
        ctx.drawTextWithShadow(tr, Text.translatable("hud.skyriders.charge"), x, y - 22, 0xFFE070);
        drawBar(ctx, x, y - 12, barW, 6, charge, charge >= 1f ? 0xFFFF4040 : 0xFFFFC020);

        // スペシャルゲージ
        float sp = m.getSpecialGauge() / 100f;
        int active = m.getSpecialActiveTicks();
        Text spLabel = active > 0 ? Text.translatable("hud.skyriders.special_active")
                : sp >= 1f ? Text.translatable("hud.skyriders.special_ready")
                : Text.translatable("hud.skyriders.special");
        ctx.drawTextWithShadow(tr, spLabel, x, y - 2, 0xFF90E0);
        boolean blink = sp >= 1f && (client.player.age / 5) % 2 == 0;
        drawBar(ctx, x, y + 8, barW, 4, active > 0 ? 1f : sp, blink ? 0xFFFFFFFF : 0xFFFF60C0);

        // ほおばり中
        if (m.hasAbility()) {
            Identifier id = Identifier.tryParse(m.getAbility());
            Text name = id != null ? Registries.ENTITY_TYPE.get(id).getName() : Text.literal(m.getAbility());
            Text t = Text.translatable("hud.skyriders.ability", name);
            ctx.drawTextWithShadow(tr, t, w / 2 - tr.getWidth(t) / 2, y - 46, 0xA0FFFF);
        }
    }

    private static void drawBar(DrawContext ctx, int x, int y, int width, int height, float fill, int color) {
        ctx.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xA0000000);
        int fw = Math.round(width * Math.max(0f, Math.min(1f, fill)));
        if (fw > 0) ctx.fill(x, y, x + fw, y + height, color);
    }
}
