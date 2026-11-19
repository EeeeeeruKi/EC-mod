package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.eeeeeeruki.minecrafttweak.feature.client.SystemLanguageFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Locale;

/**
 * 「システムの言語」。
 * 言語の画面の右上にボタンを足し、オンにするとパソコンの言語に合わせる。
 * オンのあいだは、起動したときにパソコンの言語が変わっていれば合わせ直す。
 * 言語の画面でほかの言語を選んだら、自動でオフになる。
 */
public final class SystemLanguage {
    private static String appliedCode;
    private static boolean checkedAtStart;

    private SystemLanguage() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SystemLanguage::onScreenInit);
        NeoForge.EVENT_BUS.addListener(SystemLanguage::onTick);
    }

    private static boolean enabled() {
        SystemLanguageFeature f = SystemLanguageFeature.INSTANCE;
        return f != null && f.isEnabled();
    }

    private static boolean following() {
        return enabled() && SystemLanguageFeature.INSTANCE.follow.get();
    }

    private static void setFollowing(boolean value) {
        SystemLanguageFeature.INSTANCE.follow.set(value);
        SystemLanguageFeature.INSTANCE.follow.save();
    }

    /** パソコンの言語に一番近い、マイクラにある言語のコード（例: ja_jp）。 */
    public static String systemCode(LanguageManager languages) {
        Locale locale = Locale.getDefault();
        String lang = locale.getLanguage().toLowerCase(Locale.ROOT);
        String country = locale.getCountry().toLowerCase(Locale.ROOT);
        String full = lang + "_" + country;
        if (!country.isEmpty() && languages.getLanguage(full) != null) {
            return full;
        }
        if (lang.equals("zh")) {
            // 国が分からない中国語は簡体字にする
            if (languages.getLanguage("zh_cn") != null) {
                return "zh_cn";
            }
        }
        for (String code : languages.getLanguages().keySet()) {
            if (code.startsWith(lang + "_")) {
                return code;
            }
        }
        return "en_us";
    }

    private static void applySystemLanguage(boolean reload) {
        Minecraft mc = Minecraft.getInstance();
        LanguageManager languages = mc.getLanguageManager();
        String code = systemCode(languages);
        appliedCode = code;
        if (!code.equals(languages.getSelected())) {
            MinecraftTweak.LOGGER.info("minEcraft twEEk: switching to the system language {}", code);
            languages.setSelected(code);
            mc.options.languageCode = code;
            mc.options.save();
            if (reload) {
                mc.reloadResourcePacks();
            }
        }
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen && !checkedAtStart) {
            checkedAtStart = true;
            if (following()) {
                applySystemLanguage(true);
            }
        }
        if (!(screen instanceof LanguageSelectScreen) || !enabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (following() && appliedCode == null) {
            appliedCode = systemCode(mc.getLanguageManager());
        }
        LanguageInfo info = mc.getLanguageManager().getLanguage(systemCode(mc.getLanguageManager()));
        Component name = info != null ? info.toComponent() : Component.literal("?");
        Button button = Button.builder(label(), b -> {
                    if (following()) {
                        setFollowing(false);
                        b.setMessage(label());
                    } else {
                        setFollowing(true);
                        b.setMessage(label());
                        String code = systemCode(mc.getLanguageManager());
                        appliedCode = code;
                        // 一覧でその言語を選んだ状態にする（「完了」で切り替わる）。見つからなければすぐ切り替える
                        if (!selectInList(screen, name)) {
                            applySystemLanguage(true);
                        }
                    }
                }).bounds(screen.width - 155, 6, 150, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.system_language.tooltip", name)))
                .build();
        event.addListener(button);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean selectInList(Screen screen, Component name) {
        String wanted = name.getString();
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ObjectSelectionList list) {
                for (Object e : list.children()) {
                    if (e instanceof ObjectSelectionList.Entry entry
                            && entry.getNarration().getString().contains(wanted)) {
                        list.setSelected(entry);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Component label() {
        return Component.translatable("minecrafttweak.system_language.button")
                .append(": ").append(TextSettings.onOff(following()));
    }

    private static void onTick(ClientTickEvent.Post event) {
        if (appliedCode == null || !following()) {
            return;
        }
        if (Minecraft.getInstance().screen instanceof LanguageSelectScreen) {
            return; // 言語の画面を開いているあいだは、まだ決まっていない
        }
        // 言語の画面でほかの言語を選んだら、追いかけるのをやめる
        String now = Minecraft.getInstance().options.languageCode;
        if (!appliedCode.equals(now)) {
            appliedCode = null;
            setFollowing(false);
        }
    }
}
