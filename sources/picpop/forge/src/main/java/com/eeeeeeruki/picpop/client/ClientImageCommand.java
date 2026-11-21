package com.eeeeeeruki.picpop.client;

import com.eeeeeeruki.picpop.ImageArgs;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** /imageview <URL> [横] [縦] [秒数] [大きさ%] … 自分の画面だけに出します（サーバーに PicPop が無くても使えます） */
final class ClientImageCommand {
    private ClientImageCommand() {}

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("imageview")
                .then(Commands.literal("clear").executes(ctx -> {
                    ImageManager.clearAll();
                    ctx.getSource().sendSuccess(() -> Component.translatable("picpop.command.local_cleared"), false);
                    return 1;
                }))
                .then(Commands.argument("args", StringArgumentType.greedyString())
                        .suggests(ImageArgs::suggest)
                        .executes(ctx -> {
                            ImageManager.show(ImageArgs.parse(StringArgumentType.getString(ctx, "args")));
                            return 1;
                        })));
    }
}
