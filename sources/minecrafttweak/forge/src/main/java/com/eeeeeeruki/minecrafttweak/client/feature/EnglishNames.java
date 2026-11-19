package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import java.util.List;
import java.util.Locale;

/**
 * 英語の名前を引くための辞書。言語を変えても英語（en_us）の言葉を別に読み込んでおく。
 * リソースを読み直すたびに作り直す。
 */
public final class EnglishNames {
    private static volatile Language english;

    private EnglishNames() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(EnglishNames::onRegisterReloadListeners);
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) EnglishNames::reload);
    }

    private static void reload(ResourceManager resources) {
        try {
            english = ClientLanguage.loadFrom(resources, List.of("en_us"), false);
        } catch (RuntimeException e) {
            english = null;
            MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not load the English names", e);
        }
    }

    /** いまの言語が英語なら、足す必要はない。 */
    public static boolean currentIsEnglish() {
        String code = Minecraft.getInstance().options.languageCode;
        return code == null || code.toLowerCase(Locale.ROOT).startsWith("en_");
    }

    /** 翻訳キーの英語。なければ null。 */
    public static String of(String translationKey) {
        Language en = english;
        if (en == null || translationKey == null || !en.has(translationKey)) {
            return null;
        }
        return en.getOrDefault(translationKey);
    }

    /** アイテムの英語の名前。いまの名前と同じか、分からなければ null。 */
    public static String of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String en = of(stack.getDescriptionId());
        if (en == null) {
            return null;
        }
        String now = stack.getHoverName().getString();
        return en.equalsIgnoreCase(now) ? null : en;
    }
}
