package dev.eeeeeeruki.layeredarmor.mixin.compat;

import dev.eeeeeeruki.layeredarmor.compat.AdvancedAECompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AdvancedAE：量子アーマーの設定画面を開くホットキーを、重ね着スロットの装備にも対応させる */
@Pseudo
@Mixin(targets = "net.pedroksl.advanced_ae.common.helpers.ArmorHotkeyAction", remap = false)
public abstract class AdvancedAEArmorHotkeyActionMixin {

    @Inject(method = "run(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void layeredarmor$run(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (AdvancedAECompat.isFailed()) return;
        // 失敗時は本家の処理にそのまま流す
        if (AdvancedAECompat.run(this, player)) cir.setReturnValue(true);
        else if (!AdvancedAECompat.isFailed()) cir.setReturnValue(false);
    }
}
