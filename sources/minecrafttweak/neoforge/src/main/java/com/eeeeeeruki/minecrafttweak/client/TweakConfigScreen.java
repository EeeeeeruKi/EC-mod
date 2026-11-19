package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.core.Features;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * minEcraft twEEk の設定のホーム画面（MOD一覧の「設定」・タイトル画面のボタン・Q メニューから開く）。
 * Quark の Q メニューと同じ並べ方: 1行に3つずつ、アイコン付きの分類ボタンを真ん中に並べ、
 * いちばん下に大きな「完了」ボタンを置く。最初のボタンは「設定コード」。
 */
public class TweakConfigScreen extends Screen {
    // Quark（Zeta）の Q メニューと同じ数字
    private static final int BUTTON_WIDTH = 120, BUTTON_HEIGHT = 20, GAP = 10, ROW_HEIGHT = 23, FIRST_ROW_Y = 70;

    private final Screen parent;

    public TweakConfigScreen(Screen parent) {
        super(Component.translatable("minecrafttweak.config.title"));
        this.parent = parent;
    }

    public static ItemStack icon(FeatureCategory category) {
        return new ItemStack(switch (category) {
            case TWEAKS -> Items.NAUTILUS_SHELL;
            case BUILDING -> Items.BRICKS;
            case TOOLS -> Items.IRON_PICKAXE;
            case MANAGEMENT -> Items.CHEST;
            case WORLD -> Items.GRASS_BLOCK;
            case CLIENT -> Items.ENDER_EYE;
        });
    }

    @Override
    protected void init() {
        int count = 1 + FeatureCategory.values().length;
        List<Integer> xs = new ArrayList<>();
        for (int i = 0; i < count; i += 3) {
            xs.addAll(centeredRow(width / 2, Math.min(3, count - i)));
        }

        // 1つ目: 設定コード（Quark の「一般設定」の場所）
        int x0 = xs.get(0);
        addRenderableWidget(new IconButton(x0, FIRST_ROW_Y, BUTTON_WIDTH, BUTTON_HEIGHT,
                Component.translatable("minecrafttweak.config.code_screen"), new ItemStack(Items.WRITABLE_BOOK),
                b -> open(new CodeScreen(this))))
                .setTooltip(Tooltip.create(Component.translatable("minecrafttweak.config.code_screen.tooltip")));

        // 分類
        FeatureCategory[] categories = FeatureCategory.values();
        for (int i = 0; i < categories.length; i++) {
            FeatureCategory c = categories[i];
            int slot = i + 1;
            int x = xs.get(slot);
            int y = FIRST_ROW_Y + (slot / 3) * ROW_HEIGHT;
            List<Feature> features = Features.inCategory(c);
            long on = features.stream().filter(Feature::isEnabled).count();
            Component label = Component.translatable(c.translationKey());
            if (!features.isEmpty()) {
                label = label.copy().append(Component.literal(" (" + on + "/" + features.size() + ")")
                        .withStyle(on > 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
            }
            Button button = addRenderableWidget(new IconButton(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, label, icon(c),
                    b -> open(new CategoryScreen(this, c))));
            button.active = !features.isEmpty();
            if (features.isEmpty()) {
                button.setTooltip(Tooltip.create(Component.translatable("minecrafttweak.config.empty")));
            }
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
    }

    /** n 個のボタンを、中心 center のまわりに等しい間隔で並べたときの左端（Zeta と同じ計算）。 */
    private static List<Integer> centeredRow(int center, int n) {
        int total = n * BUTTON_WIDTH + (n - 1) * GAP;
        int left = center - total / 2;
        List<Integer> xs = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            xs.add(left + i * (BUTTON_WIDTH + GAP));
        }
        return xs;
    }

    private void open(Screen screen) {
        if (minecraft != null) {
            minecraft.setScreen(screen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.home_help"), width / 2, 40, 0xAAAAAA);
    }

    Screen parentScreen() {
        return parent;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
