package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.core.Features;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 分類タブ付きの、機能オン/オフ画面（MOD一覧の「設定」から開く）。 */
public class TweakConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;
    private static final int LIST_TOP = 60;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private FeatureCategory category;
    private int page;
    private int pageCount = 1;

    private record Row(Feature feature, int x, int y) {
    }

    public TweakConfigScreen(Screen parent) {
        super(Component.translatable("minecrafttweak.config.title"));
        this.parent = parent;
        this.category = FeatureCategory.values()[0];
        for (FeatureCategory c : FeatureCategory.values()) {
            if (!Features.inCategory(c).isEmpty()) {
                this.category = c;
                break;
            }
        }
    }

    @Override
    protected void init() {
        rows.clear();

        FeatureCategory[] categories = FeatureCategory.values();
        int tabWidth = Math.min(80, (width - 20) / categories.length);
        int tabLeft = (width - tabWidth * categories.length) / 2;
        for (int i = 0; i < categories.length; i++) {
            FeatureCategory c = categories[i];
            Button tab = Button.builder(Component.translatable(c.translationKey()), b -> {
                category = c;
                page = 0;
                rebuildWidgets();
            }).bounds(tabLeft + i * tabWidth, 30, tabWidth - 2, 20).build();
            tab.active = c != category;
            addRenderableWidget(tab);
        }

        List<Feature> features = Features.inCategory(category);
        int listBottom = height - 60;
        int perPage = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        pageCount = Math.max(1, (features.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));

        int rowWidth = Math.min(320, width - 40);
        int rowLeft = (width - rowWidth) / 2;
        int start = page * perPage;
        int end = Math.min(features.size(), start + perPage);
        for (int i = start; i < end; i++) {
            Feature feature = features.get(i);
            int y = LIST_TOP + (i - start) * ROW_HEIGHT;
            Button toggle = Button.builder(stateText(feature), b -> {
                feature.setEnabled(!feature.isEnabled());
                b.setMessage(stateText(feature));
            }).bounds(rowLeft + rowWidth - 60, y, 60, 20)
                    .tooltip(Tooltip.create(Component.translatable(feature.descriptionKey())))
                    .build();
            addRenderableWidget(toggle);
            rows.add(new Row(feature, rowLeft, y));
        }

        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 70, height - 54, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);

            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 50, height - 54, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }

    private static Component stateText(Feature feature) {
        return feature.isEnabled()
                ? Component.translatable("minecrafttweak.config.on").withStyle(s -> s.withColor(0x55FF55))
                : Component.translatable("minecrafttweak.config.off").withStyle(s -> s.withColor(0xFF5555));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        for (Row row : rows) {
            graphics.drawString(font, Component.translatable(row.feature().nameKey()), row.x(), row.y() + 6, 0xFFFFFF);
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.empty"),
                    width / 2, LIST_TOP + 10, 0xAAAAAA);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 48, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
