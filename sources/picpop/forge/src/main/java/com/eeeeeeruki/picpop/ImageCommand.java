package com.eeeeeeruki.picpop;

import com.eeeeeeruki.picpop.network.PicPopNetwork;
import com.eeeeeeruki.picpop.network.ShowImagePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/** /image <プレイヤー> <URL> [横] [縦] [秒数] [大きさ%] と /image clear [プレイヤー] */
public final class ImageCommand {
    private ImageCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("image")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("clear")
                        .executes(ctx -> clear(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> clear(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("args", StringArgumentType.greedyString())
                                .suggests(ImageArgs::suggest)
                                .executes(ctx -> show(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"),
                                        ImageArgs.parse(StringArgumentType.getString(ctx, "args")))))));
    }

    private static int show(CommandSourceStack src, Collection<ServerPlayer> players, ImageArgs args) throws CommandSyntaxException {
        ShowImagePacket packet = ShowImagePacket.show(args);
        int ok = 0, missing = 0;
        for (ServerPlayer p : players) {
            if (PicPopNetwork.clientHasMod(p)) {
                PicPopNetwork.sendTo(p, packet);
                ok++;
            } else {
                missing++;
            }
        }
        final int shown = ok, miss = missing;
        src.sendSuccess(() -> Component.translatable("picpop.command.shown", shown), true);
        if (miss > 0) {
            src.sendSuccess(() -> Component.translatable("picpop.command.missing", miss).withStyle(ChatFormatting.YELLOW), false);
        }
        return shown;
    }

    private static int clear(CommandSourceStack src, Collection<ServerPlayer> players) {
        int n = 0;
        for (ServerPlayer p : players) {
            if (PicPopNetwork.clientHasMod(p)) {
                PicPopNetwork.sendTo(p, ShowImagePacket.clearAll());
                n++;
            }
        }
        final int count = n;
        src.sendSuccess(() -> Component.translatable("picpop.command.cleared", count), true);
        return count;
    }
}
