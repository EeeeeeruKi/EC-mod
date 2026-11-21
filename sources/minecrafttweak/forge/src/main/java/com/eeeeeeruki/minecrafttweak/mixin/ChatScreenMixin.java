package com.eeeeeeruki.minecrafttweak.mixin;

import com.eeeeeeruki.minecrafttweak.client.feature.ChatImages;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** チャットの画像: 画像を添付しているときは、送信で画像を送る。 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void minecrafttweak$sendImage(String message, boolean addToRecentChat, CallbackInfoReturnable<Boolean> cir) {
        if (ChatImages.onSend(message)) {
            cir.setReturnValue(true);
        }
    }
}
