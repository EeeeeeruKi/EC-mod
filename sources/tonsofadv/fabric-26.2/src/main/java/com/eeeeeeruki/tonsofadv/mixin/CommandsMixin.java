package com.eeeeeeruki.tonsofadv.mixin;

import com.eeeeeeruki.tonsofadv.TonsOfAdv;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** コマンドを使ったとき */
@Mixin(Commands.class)
public class CommandsMixin {
    @Inject(method = "performCommand", at = @At("HEAD"), require = 0)
    private void tonsofadv$command(ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        TonsOfAdv.add(parse.getContext().getSource().getPlayer(), "commands_used", 1);
    }
}
