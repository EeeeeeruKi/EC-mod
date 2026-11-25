package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.client.McreatorFinderFeature;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.forgespi.language.IModInfo;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * MCreator製modさがし。
 * MCreator で作ったmodは、ふつう次のどちらかでわかる。
 * - クラスが net/mcreator/ の下にある（MCreator が最初に付けるパッケージ）
 * - mods.toml / neoforge.mods.toml に「MCreator」と書いてある（credits の「Created using mod maker MCreator」など）
 */
public final class McreatorFinder {
    private static final Map<Path, Boolean> CACHE = new HashMap<>();
    private static boolean registered;

    private McreatorFinder() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent e) -> registerCommand(e.getDispatcher()));
    }

    private static boolean enabled() {
        return McreatorFinderFeature.INSTANCE != null && McreatorFinderFeature.INSTANCE.isEnabled();
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mcreator")
                .executes(ctx -> run(true))
                .then(Commands.literal("check").executes(ctx -> run(false))));
    }

    private static int run(boolean toChat) {
        Minecraft mc = Minecraft.getInstance();
        if (!enabled()) {
            mc.gui.getChat().addMessage(Component.translatable("minecrafttweak.mcreator.off").withStyle(ChatFormatting.RED));
            return 0;
        }
        List<String> names = find();
        String text = names.isEmpty()
                ? I18n.get("minecrafttweak.mcreator.none")
                : I18n.get("minecrafttweak.mcreator.found", names.size(), String.join(", ", names));
        if (toChat && mc.getConnection() != null) {
            LongChat.sendText(text);
        } else {
            mc.gui.getChat().addMessage(Component.literal(text).withStyle(ChatFormatting.YELLOW));
        }
        return names.size();
    }

    /** MCreator製のmodの名前（同じjarのmodは1つにまとめる）。 */
    public static List<String> find() {
        Map<Path, String> byFile = new LinkedHashMap<>();
        for (IModInfo info : ModList.get().getMods()) {
            String id = info.getModId();
            if (id.equals("minecraft") || id.equals("neoforge") || id.equals("forge")) {
                continue;
            }
            Path path;
            try {
                path = info.getOwningFile().getFile().getFilePath();
            } catch (Throwable t) {
                continue;
            }
            if (path == null || byFile.containsKey(path)) {
                continue;
            }
            if (isMcreator(path)) {
                byFile.put(path, info.getDisplayName());
            }
        }
        List<String> names = new ArrayList<>(byFile.values());
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private static boolean isMcreator(Path path) {
        return CACHE.computeIfAbsent(path, p -> {
            if (!Files.isRegularFile(p) || !p.toString().toLowerCase(Locale.ROOT).endsWith(".jar")) {
                return false;
            }
            try (ZipFile zip = new ZipFile(p.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (e.getName().startsWith("net/mcreator/")) {
                        return true;
                    }
                }
                for (String toml : new String[]{"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
                    ZipEntry e = zip.getEntry(toml);
                    if (e != null && e.getSize() < 1_000_000) {
                        try (InputStream in = zip.getInputStream(e)) {
                            String s = new String(in.readAllBytes(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
                            if (s.contains("mcreator")) {
                                return true;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // 読めないファイルは飛ばす
            }
            return false;
        });
    }
}
