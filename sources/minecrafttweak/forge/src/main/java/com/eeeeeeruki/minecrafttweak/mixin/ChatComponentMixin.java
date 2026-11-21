package com.eeeeeeruki.minecrafttweak.mixin;

import com.eeeeeeruki.minecrafttweak.client.feature.ChatImages;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Style;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** チャットの画像: メッセージの下に画像の行を足し、その行は文字のかわりに画像を描く。 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;ILnet/minecraft/client/GuiMessageTag;Z)V",
            at = @At("TAIL"))
    private void minecrafttweak$addImageLines(Component content, MessageSignature signature, int addedTime, GuiMessageTag tag,
                                             boolean onlyTrim, CallbackInfo ci) {
        List<FormattedCharSequence> lines = ChatImages.linesFor(content, (ChatComponent) (Object) this);
        if (lines.isEmpty()) {
            return;
        }
        for (FormattedCharSequence line : lines) {
            trimmedMessages.add(0, new GuiMessage.Line(addedTime, line, null, false));
        }
        while (trimmedMessages.size() > 100) {
            trimmedMessages.remove(trimmedMessages.size() - 1);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;III)I"))
    private int minecrafttweak$drawImageLine(GuiGraphics graphics, Font font, FormattedCharSequence text, int x, int y, int color) {
        if (text instanceof ChatImages.ImageLine line) {
            ChatImages.draw(graphics, line, x, y, color);
            return 0;
        }
        return graphics.drawString(font, text, x, y, color);
    }

    @Shadow
    private double screenToChatX(double x) {
        throw new AssertionError();
    }

    @Shadow
    private double screenToChatY(double y) {
        throw new AssertionError();
    }

    @Shadow
    private int getMessageLineIndexAt(double chatX, double chatY) {
        throw new AssertionError();
    }

    /** PicPop 連携: チャットの画像にマウスを乗せると説明が出て、クリックすると画面の中央に大きく表示する。 */
    @Inject(method = "getClickedComponentStyleAt", at = @At("RETURN"), cancellable = true)
    private void minecrafttweak$picpopImageStyle(double x, double y, CallbackInfoReturnable<Style> cir) {
        if (cir.getReturnValue() != null || !(Minecraft.getInstance().screen instanceof ChatScreen)) {
            return;
        }
        double chatX = screenToChatX(x);
        double chatY = screenToChatY(y);
        int index = getMessageLineIndexAt(chatX, chatY);
        if (index < 0 || index >= trimmedMessages.size()) {
            return;
        }
        Style style = ChatImages.picpopStyle(trimmedMessages.get(index).content(), chatX);
        if (style != null) {
            cir.setReturnValue(style);
        }
    }
}
