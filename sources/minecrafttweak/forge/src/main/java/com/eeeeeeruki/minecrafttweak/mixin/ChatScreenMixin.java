package com.eeeeeeruki.minecrafttweak.mixin;

import com.eeeeeeruki.minecrafttweak.client.feature.ChatImages;
import com.eeeeeeruki.minecrafttweak.client.feature.LongChat;
import net.minecraft.client.gui.screens.ChatScreen;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * チャット画面。
 * - チャットの画像: 画像を添付しているときは、送信で画像を送る。
 * - チャットの文字数を増やす: 入力欄の上限を変え、256文字を超えた文はこのmodの送り方で送る。
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void minecrafttweak$send(String message, boolean addToRecentChat, CallbackInfoReturnable<Boolean> cir) {
        if (ChatImages.onSend(message)) {
            cir.setReturnValue(true);
            return;
        }
        if (LongChat.onSend(((ChatScreen) (Object) this).normalizeChatMessage(message), addToRecentChat)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyConstant(method = "init", constant = @Constant(intValue = 256))
    private int minecrafttweak$maxLength(int original) {
        return LongChat.maxLength();
    }

    @Inject(method = "normalizeChatMessage", at = @At("HEAD"), cancellable = true)
    private void minecrafttweak$normalize(String message, CallbackInfoReturnable<String> cir) {
        if (LongChat.enabled()) {
            String s = StringUtils.normalizeSpace(message.trim());
            int max = LongChat.maxLength();
            cir.setReturnValue(s.length() > max ? s.substring(0, max) : s);
        }
    }
}
