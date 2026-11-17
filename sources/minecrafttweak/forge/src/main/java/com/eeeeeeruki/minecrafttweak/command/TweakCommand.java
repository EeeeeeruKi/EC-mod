package com.eeeeeeruki.minecrafttweak.command;

import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * /minecrafttweak list … 番号つきの一覧と、今の設定の文字列を表示
 * /minecrafttweak set &lt;テキスト&gt; … 例: /minecrafttweak set 1 t 1.1 0
 */
public final class TweakCommand {
    private TweakCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("minecrafttweak")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> {
                    CommandSourceStack source = ctx.getSource();
                    source.sendSuccess(() -> Component.translatable("minecrafttweak.command.list_header")
                            .withStyle(ChatFormatting.AQUA), false);
                    for (Component line : TextSettings.listLines()) {
                        source.sendSuccess(() -> line, false);
                    }
                    String now = TextSettings.currentText();
                    source.sendSuccess(() -> Component.translatable("minecrafttweak.command.current",
                            Component.literal(now).withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.GRAY), false);
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("text", StringArgumentType.greedyString()).executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            TextSettings.Result result = TextSettings.apply(StringArgumentType.getString(ctx, "text"));
                            for (Component msg : result.messages()) {
                                source.sendSuccess(() -> msg, true);
                            }
                            for (Component err : result.errors()) {
                                source.sendFailure(err);
                            }
                            return result.changed();
                        }))));
    }
}
