package com.eeeeeeruki.minecrafttweak.command;

import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * /minecrafttweak list … 番号つきの一覧と、今の設定コードを表示
 * /minecrafttweak import &lt;設定コード&gt; … 例: /minecrafttweak import 1-t,1a-0
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
                            Component.literal(now).withStyle(s -> s.withColor(ChatFormatting.WHITE)
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, now))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                            Component.translatable("minecrafttweak.command.click_copy")))))
                            .withStyle(ChatFormatting.GRAY), false);
                    return 1;
                }))
                .then(Commands.literal("import")
                        .then(Commands.argument("code", StringArgumentType.greedyString()).executes(ctx -> {
                            CommandSourceStack source = ctx.getSource();
                            TextSettings.Result result = TextSettings.apply(StringArgumentType.getString(ctx, "code"));
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
