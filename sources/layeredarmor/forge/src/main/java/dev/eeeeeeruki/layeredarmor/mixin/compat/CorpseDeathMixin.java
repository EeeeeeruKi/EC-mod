package dev.eeeeeeruki.layeredarmor.mixin.compat;

import dev.eeeeeeruki.layeredarmor.compat.CorpseCompat;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Corpse：死体の見た目（装備のコピー）に重ね着の防具を反映する */
@Pseudo
@Mixin(targets = "de.maxhenkel.corpse.corelib.death.Death", remap = false)
public abstract class CorpseDeathMixin {

    @Inject(method = "fromPlayer(Lnet/minecraft/world/entity/player/Player;)Lde/maxhenkel/corpse/corelib/death/Death;",
            at = @At("RETURN"), require = 0, remap = false)
    private static void layeredarmor$fromPlayer(Player player, CallbackInfoReturnable<Object> cir) {
        CorpseCompat.applyToDeath(player, cir.getReturnValue());
    }
}
