package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import com.eeeeeeruki.minecrafttweak.feature.client.SystemLanguageFeature;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 「システムの言語」。
 * 機能がオンのとき、言語の画面の一覧のいちばん上に「システムの言語」を足す。
 * それを選ぶとパソコンの言語に合わせ、次に起動したときもパソコンの言語が変わっていれば合わせ直す。
 * 一覧でほかの言語を選ぶと、合わせるのをやめる。
 */
public final class SystemLanguage {
    private static boolean checkedAtStart;
    /** 起動時の読み込み（ロード画面）が終わってから言語を切り替えるための印。 */
    private static boolean pendingApply;
    private static int readyTicks;
    /** いま開いている言語の画面に足した「システムの言語」の項目。 */
    private static Object addedEntry;
    private static ObjectSelectionList<?> addedList;

    private SystemLanguage() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(SystemLanguage::onScreenInit);
        MinecraftForge.EVENT_BUS.addListener(SystemLanguage::onScreenClosing);
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent e) -> {
            if (e.phase == net.minecraftforge.event.TickEvent.Phase.END) onTick();
        });
    }

    private static boolean enabled() {
        SystemLanguageFeature f = SystemLanguageFeature.INSTANCE;
        return f != null && f.isEnabled();
    }

    // ------------------------------------------------------------------ 「合わせる」の保存

    private static Path stateFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("minecrafttweak-system-language.txt");
    }

    private static boolean following() {
        if (!enabled()) return false;
        try {
            Path f = stateFile();
            if (!Files.exists(f)) {
                // まだ決めていない（初めて起動したとき）: 言語を自分で選んでいなければ（最初の英語のままなら）、システムの言語に合わせる
                boolean first = "en_us".equals(Minecraft.getInstance().options.languageCode);
                setFollowing(first);
                return first;
            }
            return Files.exists(f) && Files.readString(f, StandardCharsets.UTF_8).trim().equals("true");
        } catch (IOException e) {
            return false;
        }
    }

    private static void setFollowing(boolean value) {
        try {
            Path f = stateFile();
            Files.createDirectories(f.getParent());
            Files.writeString(f, value ? "true" : "false", StandardCharsets.UTF_8);
        } catch (IOException e) {
            MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not save the system language setting", e);
        }
    }

    // ------------------------------------------------------------------ パソコンの言語

    /** パソコンの言語に一番近い、マイクラにある言語のコード（例: ja_jp）。 */
    public static String systemCode(LanguageManager languages) {
        String[] lc = OsLanguage.get();
        String lang = lc[0];
        String country = lc[1];
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

    private static void onTick() {
        if (!pendingApply) {
            return;
        }
        // 読み込みがすべて終わって、少し落ち着いてから（1秒）切り替える
        if (!SoundReady.ready()) {
            readyTicks = 0;
            return;
        }
        if (++readyTicks < 20) {
            return;
        }
        pendingApply = false;
        applySystemLanguage();
    }

    private static void applySystemLanguage() {
        Minecraft mc = Minecraft.getInstance();
        LanguageManager languages = mc.getLanguageManager();
        String code = systemCode(languages);
        if (!code.equals(languages.getSelected())) {
            MinecraftTweak.LOGGER.info("minEcraft twEEk: switching to the system language {}", code);
            languages.setSelected(code);
            mc.options.languageCode = code;
            mc.options.save();
            mc.reloadResourcePacks();
        }
    }

    // ------------------------------------------------------------------ 画面

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen && !checkedAtStart) {
            checkedAtStart = true;
            // ここではまだ起動時の読み込みの途中のことがある。そのときに読み込み直すと文字や絵が壊れるので、
            // 読み込みが終わってから（onTick で）切り替える
            pendingApply = following();
        }
        if (!(screen instanceof LanguageSelectScreen) || !enabled()) {
            return;
        }
        addedEntry = null;
        addedList = null;
        ObjectSelectionList<?> list = languageList(screen);
        if (list == null || list.children().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String code = systemCode(mc.getLanguageManager());
        LanguageInfo real = mc.getLanguageManager().getLanguage(code);
        String nameOfLanguage = real != null ? real.name() : code;
        // 表示は「システムの言語 (日本語)」になる（LanguageInfo の表示は「名前 (地域)」の形）
        LanguageInfo shown = new LanguageInfo(nameOfLanguage,
                Component.translatable("minecrafttweak.system_language.entry").getString(),
                real != null && real.bidirectional());
        Object entry = newEntry(list, code, shown);
        if (entry == null) {
            return;
        }
        addFirst(list, entry);
        addedEntry = entry;
        addedList = list;
        if (following()) {
            select(list, entry);
        }
    }

    /** 言語の画面を閉じるとき、どれを選んで閉じたかで「合わせる」を決める。 */
    private static void onScreenClosing(ScreenEvent.Closing event) {
        if (!(event.getScreen() instanceof LanguageSelectScreen) || addedList == null) {
            return;
        }
        Object selected = addedList.getSelected();
        Minecraft mc = Minecraft.getInstance();
        if (selected == addedEntry) {
            // 「完了」で決めたときだけ（Esc で閉じたときは言語が変わっていない）
            if (systemCode(mc.getLanguageManager()).equals(mc.options.languageCode)) {
                setFollowing(true);
            }
        } else if (selected != null) {
            setFollowing(false);
        }
        addedEntry = null;
        addedList = null;
    }

    // ------------------------------------------------------------------ バニラの一覧を触るところ

    private static ObjectSelectionList<?> languageList(Screen screen) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof ObjectSelectionList<?> list) {
                return list;
            }
        }
        return null;
    }

    /**
     * バニラの一覧の項目と同じ種類の項目を作る。項目のクラスは外から見えないので、
     * 既存の項目のクラスからコンストラクタ（一覧, 言語コード, 言語の情報）を探して使う。
     */
    private static Object newEntry(ObjectSelectionList<?> list, String code, LanguageInfo info) {
        Class<?> entryClass = list.children().get(0).getClass();
        for (Constructor<?> c : entryClass.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            try {
                c.setAccessible(true);
                if (p.length == 3 && p[0].isInstance(list) && p[1] == String.class && p[2] == LanguageInfo.class) {
                    return c.newInstance(list, code, info);
                }
                if (p.length == 3 && p[0].isInstance(list) && p[1] == String.class && Component.class.isAssignableFrom(p[2])) {
                    return c.newInstance(list, code, info.toComponent());
                }
                if (p.length == 2 && p[0] == String.class && p[1] == LanguageInfo.class) {
                    return c.newInstance(code, info);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                MinecraftTweak.LOGGER.warn("minEcraft twEEk: could not add the system language entry", e);
                return null;
            }
        }
        MinecraftTweak.LOGGER.warn("minEcraft twEEk: no usable constructor in {}", entryClass.getName());
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addFirst(ObjectSelectionList<?> list, Object entry) {
        ((List) list.children()).add(0, entry);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void select(ObjectSelectionList<?> list, Object entry) {
        ((ObjectSelectionList) list).setSelected((ObjectSelectionList.Entry) entry);
        list.setScrollAmount(0);
    }
}
