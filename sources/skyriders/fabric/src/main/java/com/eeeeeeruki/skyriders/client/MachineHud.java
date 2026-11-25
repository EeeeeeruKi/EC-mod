package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.entity.PatchKind;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** HP・チャージ・スペシャル・速度・パッチの段階・着地の表示 */
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
        ctx.drawTextWithShadow(tr, speedText, w / 2 - tr.getWidth(speedText) / 2, y - 46, 0xFFFFFF);

        // HP
        float hpRate = m.getHp() / m.maxHp();
        Text hpText = Text.translatable("hud.skyriders.hp", Math.round(m.getHp()), Math.round(m.maxHp()));
        ctx.drawTextWithShadow(tr, hpText, x, y - 34, 0xA0FFA0);
        int hpColor = hpRate > 0.5f ? 0xFF40E060 : hpRate > 0.25f ? 0xFFFFC020 : 0xFFFF4040;
        drawBar(ctx, x, y - 24, barW, 4, hpRate, hpColor);

        // チャージゲージ
        float charge = m.getCharge();
        ctx.drawTextWithShadow(tr, Text.translatable("hud.skyriders.charge"), x, y - 16, 0xFFE070);
        drawBar(ctx, x, y - 6, barW, 5, charge, charge >= 1f ? 0xFFFF4040 : 0xFFFFC020);

        // スペシャルゲージ
        float sp = m.getSpecialGauge() / 100f;
        int active = m.getSpecialActiveTicks();
        Text spLabel = active > 0 ? Text.translatable("hud.skyriders.special_active")
                : sp >= 1f ? Text.translatable("hud.skyriders.special_ready")
                : Text.translatable("hud.skyriders.special");
        ctx.drawTextWithShadow(tr, spLabel, x, y + 2, 0xFF90E0);
        boolean blink = sp >= 1f && (client.player.age / 5) % 2 == 0;
        drawBar(ctx, x, y + 12, barW, 3, active > 0 ? 1f : sp, blink ? 0xFFFFFFFF : 0xFFFF60C0);

        // パッチの段階（右はし）
        int px = w - 84;
        int py = h / 2 - PatchKind.ALL.length * 5 - 6;
        ctx.fill(px - 4, py - 4, w - 4, py + PatchKind.ALL.length * 10 + 2, 0x80000000);
        for (PatchKind k : PatchKind.ALL) {
            int lv = m.level(k);
            ctx.fill(px, py + 2, px + 4, py + 6, 0xFF000000 | k.color);
            ctx.drawTextWithShadow(tr, k.displayName(), px + 7, py, 0xFFFFFF);
            String s = lv > 0 ? "+" + lv : String.valueOf(lv);
            int c = lv > 0 ? 0x70FF70 : lv < 0 ? 0xFF7070 : 0xC0C0C0;
            ctx.drawTextWithShadow(tr, s, w - 8 - tr.getWidth(s), py, c);
            py += 10;
        }

        // 失速
        if (m.isStalled() && (client.player.age / 4) % 2 == 0) {
            Text t = Text.translatable("hud.skyriders.stall");
            ctx.drawTextWithShadow(tr, t, w / 2 - tr.getWidth(t) / 2, h / 2 + 20, 0xFF8040);
        }

        // 上昇の限界
        if (m.isOverheated()) {
            Text t = Text.translatable("hud.skyriders.overheat");
            ctx.drawTextWithShadow(tr, t, w / 2 - tr.getWidth(t) / 2, h / 2 + 32, 0xFFB040);
        }

        // ナイス着地／着地失敗（通常着地は表示しない）
        int landing = m.getLandingResult();
        if (landing == 1 || landing == 2) {
            Text t = Text.translatable(landing == 1 ? "hud.skyriders.nice_landing" : "hud.skyriders.bad_landing");
            ctx.getMatrices().push();
            ctx.getMatrices().translate(w / 2f, h / 2f - 40f, 0f);
            ctx.getMatrices().scale(2f, 2f, 1f);
            ctx.drawTextWithShadow(tr, t, -tr.getWidth(t) / 2, 0, landing == 1 ? 0xFFE040 : 0xFF6060);
            ctx.getMatrices().pop();
        }

        // ほおばり中
        if (m.hasAbility()) {
            Identifier id = Identifier.tryParse(m.getAbility());
            Text name = id != null ? Registries.ENTITY_TYPE.get(id).getName() : Text.literal(m.getAbility());
            Text t = Text.translatable("hud.skyriders.ability", name);
            ctx.drawTextWithShadow(tr, t, w / 2 - tr.getWidth(t) / 2, y - 58, 0xA0FFFF);
        }
    }

    private static void drawBar(DrawContext ctx, int x, int y, int width, int height, float fill, int color) {
        ctx.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xA0000000);
        int fw = Math.round(width * Math.max(0f, Math.min(1f, fill)));
        if (fw > 0) ctx.fill(x, y, x + fw, y + height, color);
    }
}
