package com.eeeeeeruki.minecrafttweak.client.feature;

import com.eeeeeeruki.minecrafttweak.feature.fixes.RecipeViewerFtbFixFeature;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * 画面にある FTB 系のボタン（FTB Library のサイドバーなど、パッケージが dev.ftb. のもの）の、今の場所。
 * EMI・JEI に「ここは避けて」と伝えるのに使う。まわりに少しすき間をあける。
 */
public final class FtbButtons {
    private static final int MARGIN = 2;

    private FtbButtons() {
    }

    /** {x, y, 幅, 高さ} の一覧。 */
    public static List<int[]> areas(Screen screen) {
        List<int[]> out = new ArrayList<>();
        if (screen == null || !RecipeViewerFtbFixFeature.active()) {
            return out;
        }
        try {
            for (GuiEventListener child : screen.children()) {
                if (child instanceof AbstractWidget w && w.visible && w.getWidth() > 0 && w.getHeight() > 0
                        && w.getClass().getName().startsWith("dev.ftb.")) {
                    out.add(new int[]{w.getX() - MARGIN, w.getY() - MARGIN, w.getWidth() + MARGIN * 2, w.getHeight() + MARGIN * 2});
                }
            }
        } catch (Throwable ignored) {
            // 画面の作りが変わっていても止めない
        }
        return out;
    }
}
