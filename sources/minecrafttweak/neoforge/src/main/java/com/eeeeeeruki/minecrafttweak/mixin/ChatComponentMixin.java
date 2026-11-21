package com.eeeeeeruki.minecrafttweak.mixin;

import com.eeeeeeruki.minecrafttweak.client.feature.ChatImages;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** チャットの画像: メッセージの下に画像の行を足し、その行は文字のかわりに画像を描く。 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
    private void minecrafttweak$addImageLines(GuiMessage message, CallbackInfo ci) {
        List<FormattedCharSequence> lines = ChatImages.linesFor(message.content(), (ChatComponent) (Object) this);
        if (lines.isEmpty()) {
            return;
        }
        for (FormattedCharSequence line : lines) {
            trimmedMessages.add(0, new GuiMessage.Line(message.addedTime(), line, null, false));
        }
        while (trimmedMessages.size() > 100) {
            trimmedMessages.remove(trimmedMessages.size() - 1);
        }
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int minecrafttweak$drawImageLine(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color,
                                            Operation<Integer> original) {
        if (text instanceof ChatImages.ImageLine line) {
            ChatImages.draw(graphics, line, x, y, color);
            return 0;
        }
        return original.call(graphics, font, text, x, y, color);
    }
}
