package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.MinecraftTweak;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

/**
 * Quark の設定画面（Q メニュー）に、Quark の分類と同じ見た目の「minEcraft twEEk」ボタンを足す。
 * 押すと、このMODの設定画面が開く。
 *
 * <p>Q メニューは Zeta（Quark の土台）の画面で、分類は Quark の中で決め打ちになっている。
 * そこに分類を足す仕組みはないので、画面ができたあとにボタンを足す。Quark と Zeta は
 * コンパイルのときには使わず、クラス名とリフレクションで探す（入っていなければ何もしない）。</p>
 */
public final class QuarkMenuButton {
    private static final String HOME_SCREEN = "org.violetmoon.zeta.client.config.screen.ZetaConfigHomeScreen";
    private static final String ZETA_SCREEN = "org.violetmoon.zeta.client.config.screen.ZetaScreen";
    private static final String CATEGORY_BUTTON = "org.violetmoon.zeta.client.config.widget.CategoryButton";
    // Zeta の Q メニューの並べ方と同じ数字（1行に3つ、1段 23、ボタンの幅 120（うち 20 はチェック欄））
    private static final int ROW_HEIGHT = 23, FIRST_ROW_Y = 70, WIDTH = 120, HEIGHT = 20;

    private QuarkMenuButton() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(QuarkMenuButton::onInit);
    }

    private static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!screen.getClass().getName().equals(HOME_SCREEN) || !isQuarkMenu(screen)) {
            return;
        }
        // いちばん下の分類ボタンの1段下、真ん中に置く
        int lowest = -1;
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget w && w.getClass().getName().equals(CATEGORY_BUTTON)) {
                lowest = Math.max(lowest, w.getY());
            }
        }
        int y = lowest < 0 ? FIRST_ROW_Y : lowest + ROW_HEIGHT;
        int x = screen.width / 2 - WIDTH / 2;
        Component label = Component.translatable("minecrafttweak.quark_menu.button");
        Button.OnPress open = b -> Minecraft.getInstance().setScreen(new TweakConfigScreen(screen));
        Button button = categoryButton(x, y, label, new ItemStack(Items.COMPARATOR), open);
        if (button == null) {
            button = Button.builder(label, open).bounds(x, y, WIDTH, HEIGHT).build();
        }
        button.setTooltip(Tooltip.create(Component.translatable("minecrafttweak.quark_menu.tooltip")));
        event.addListener(button);
    }

    /** Q メニューが Quark のものか（ほかの Zeta のMODの設定画面には足さない）。 */
    private static boolean isQuarkMenu(Screen screen) {
        try {
            Class<?> zetaScreen = Class.forName(ZETA_SCREEN, false, screen.getClass().getClassLoader());
            Field zField = zetaScreen.getDeclaredField("z");
            zField.setAccessible(true);
            Object zeta = zField.get(screen);
            Field modid = zeta.getClass().getField("modid");
            return "quark".equals(modid.get(zeta));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** Zeta の分類ボタン（左にアイコンが付く）を作る。作れなければ null。 */
    private static Button categoryButton(int x, int y, Component label, ItemStack icon, Button.OnPress onPress) {
        try {
            Class<?> cls = Class.forName(CATEGORY_BUTTON);
            Constructor<?> c = cls.getConstructor(int.class, int.class, int.class, int.class,
                    Component.class, ItemStack.class, Button.OnPress.class);
            return (Button) c.newInstance(x, y, WIDTH, HEIGHT, label, icon, onPress);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            MinecraftTweak.LOGGER.debug("minEcraft twEEk: using a plain button in the Q menu", e);
            return null;
        }
    }
}
