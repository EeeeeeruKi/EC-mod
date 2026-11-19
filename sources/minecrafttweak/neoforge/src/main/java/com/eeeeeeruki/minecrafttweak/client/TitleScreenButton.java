package com.eeeeeeruki.minecrafttweak.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * タイトル画面に、Quark の「q」ボタンと同じような小さいボタンを足す。押すと設定のホームが開く。
 * 言語ボタンの左か、アクセシビリティのボタンの右の、あいているほうに置く（Quark のボタンとは重ならない）。
 */
public final class TitleScreenButton {
    private TitleScreenButton() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, TitleScreenButton::onInit);
    }

    private static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof TitleScreen)) {
            return;
        }
        List<? extends GuiEventListener> children = screen.children();
        // 言語・アクセシビリティのボタン（20×20）が並ぶ段: 「シングルプレイ」の 72 下
        int row = screen.height / 4 + 48 + 72 + 12;
        int[][] candidates = {
                {screen.width / 2 - 124 - 24, row},  // 言語ボタンの左
                {screen.width / 2 + 104 + 24, row},  // アクセシビリティの右
                {screen.width / 2 - 124 - 48, row},
                {screen.width / 2 + 104 + 48, row},
        };
        for (int[] c : candidates) {
            if (free(children, c[0], c[1])) {
                Button button = new IconButton(c[0], c[1], 20, 20, Component.empty(), new ItemStack(Items.COMPARATOR),
                        b -> Minecraft.getInstance().setScreen(new TweakConfigScreen(screen)));
                button.setTooltip(Tooltip.create(Component.translatable("minecrafttweak.title_button")));
                event.addListener(button);
                return;
            }
        }
    }

    private static boolean free(List<? extends GuiEventListener> children, int x, int y) {
        for (GuiEventListener child : children) {
            if (child instanceof AbstractWidget w && w.visible
                    && x < w.getX() + w.getWidth() && x + 20 > w.getX()
                    && y < w.getY() + w.getHeight() && y + 20 > w.getY()) {
                return false;
            }
        }
        return true;
    }
}
