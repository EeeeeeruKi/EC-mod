package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 1つの分類の機能の一覧（Quark の分類の画面と同じ形）。
 * 1行に1つの機能: 左に番号と名前、右にオン/オフと「詳細」。いちばん下に大きな「完了」。
 */
public class CategoryScreen extends Screen {
    private static final int ROW_HEIGHT = 24, LIST_TOP = 40;

    private final Screen parent;
    private final FeatureCategory category;
    private final List<Row> rows = new ArrayList<>();
    private int page, pageCount = 1;

    private record Row(Feature feature, int x, int y, int width) {
    }

    public CategoryScreen(Screen parent, FeatureCategory category) {
        super(Component.translatable(category.translationKey()));
        this.parent = parent;
        this.category = category;
    }

    @Override
    protected void init() {
        rows.clear();
        List<Feature> all = Features.inCategory(category);
        int rowWidth = Math.min(340, width - 40);
        int rowLeft = (width - rowWidth) / 2;
        int listBottom = height - 64;
        int perPage = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        pageCount = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * perPage, end = Math.min(all.size(), start + perPage);
        for (int i = start; i < end; i++) {
            Feature feature = all.get(i);
            int y = LIST_TOP + (i - start) * ROW_HEIGHT;
            rows.add(new Row(feature, rowLeft, y, rowWidth));
            addRenderableWidget(Button.builder(TextSettings.onOff(feature.isEnabled()), b -> {
                        feature.setEnabled(!feature.isEnabled());
                        rebuildWidgets();
                    }).bounds(rowLeft + rowWidth - 60, y, 60, 20)
                    .tooltip(Tooltip.create(Component.translatable(feature.descriptionKey())))
                    .build());
            if (!feature.options().isEmpty()) {
                addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.details"), b -> {
                            if (minecraft != null) {
                                minecraft.setScreen(new FeatureDetailScreen(this, feature));
                            }
                        }).bounds(rowLeft + rowWidth - 124, y, 62, 20)
                        .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.details.tooltip")))
                        .build());
            }
        }
        if (pageCount > 1) {
            Button prev = addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 60, height - 56, 20, 20).build());
            prev.active = page > 0;
            Button next = addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 40, height - 56, 20, 20).build());
            next.active = page < pageCount - 1;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.renderItem(TweakConfigScreen.icon(category), width / 2 - font.width(title) / 2 - 20, 10);
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        for (Row row : rows) {
            Component label = Component.literal(row.feature().number() + ". ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.translatable(row.feature().nameKey()).withStyle(ChatFormatting.WHITE));
            graphics.drawString(font, label, row.x(), row.y() + 6, 0xFFFFFF);
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.empty"), width / 2, LIST_TOP + 10, 0xAAAAAA);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 50, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
