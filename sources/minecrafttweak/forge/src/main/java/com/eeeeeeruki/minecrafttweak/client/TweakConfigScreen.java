package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import com.eeeeeeruki.minecrafttweak.core.FeatureOption;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 分類タブ付きの設定画面（MOD一覧の「設定」から開く）。
 * 下の欄に設定コード（例: 1-t,1a-0,v1-12,s3-f）を貼って「インポート」を押すと、まとめて変更できる。
 * 「コピー」で今の設定コードをクリップボードに写す。
 */
public class TweakConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 22;
    private static final int LIST_TOP = 58;

    private final Screen parent;
    private final List<Row> rows = new ArrayList<>();
    private FeatureCategory category;
    private int page;
    private int pageCount = 1;
    private EditBox textBox;
    private String pendingText;
    private Component resultMessage = Component.empty();

    /** feature 行（ボタンあり）か option 行（値の表示だけ）。 */
    private record Row(Feature feature, FeatureOption option, int x, int y, int width) {
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

        // 分類タブ
        FeatureCategory[] categories = FeatureCategory.values();
        int tabWidth = Math.min(80, (width - 20) / categories.length);
        int tabLeft = (width - tabWidth * categories.length) / 2;
        for (int i = 0; i < categories.length; i++) {
            FeatureCategory c = categories[i];
            Button tab = Button.builder(Component.translatable(c.translationKey()), b -> {
                category = c;
                page = 0;
                keepTextAndRebuild();
            }).bounds(tabLeft + i * tabWidth, 30, tabWidth - 2, 20).build();
            tab.active = c != category;
            addRenderableWidget(tab);
        }

        // 行を並べる（機能の行のあとに、その機能の追加設定の行）
        List<Row> all = new ArrayList<>();
        int rowWidth = Math.min(320, width - 40);
        int rowLeft = (width - rowWidth) / 2;
        for (Feature feature : Features.inCategory(category)) {
            all.add(new Row(feature, null, rowLeft, 0, rowWidth));
        }

        int listBottom = height - 100;
        int perPage = Math.max(1, (listBottom - LIST_TOP) / ROW_HEIGHT);
        pageCount = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * perPage;
        int end = Math.min(all.size(), start + perPage);
        for (int i = start; i < end; i++) {
            Row base = all.get(i);
            int y = LIST_TOP + (i - start) * ROW_HEIGHT;
            Row row = new Row(base.feature(), base.option(), base.x(), y, base.width());
            rows.add(row);
            if (row.option() == null) {
                Feature feature = row.feature();
                addRenderableWidget(Button.builder(TextSettings.onOff(feature.isEnabled()), b -> {
                            feature.setEnabled(!feature.isEnabled());
                            keepTextAndRebuild();
                        }).bounds(row.x() + row.width() - 60, y, 60, 20)
                        .tooltip(Tooltip.create(Component.translatable(feature.descriptionKey())))
                        .build());
                if (!feature.options().isEmpty()) {
                    addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.details"), b -> {
                                if (minecraft != null) {
                                    minecraft.setScreen(new FeatureDetailScreen(this, feature));
                                }
                            }).bounds(row.x() + row.width() - 124, y, 62, 20)
                            .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.details.tooltip")))
                            .build());
                }
            }
        }

        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                page--;
                keepTextAndRebuild();
            }).bounds(width / 2 - 60, height - 96, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);

            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                keepTextAndRebuild();
            }).bounds(width / 2 + 40, height - 96, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }

        // 設定コード欄
        int boxWidth = Math.min(280, width - 150);
        int boxLeft = (width - boxWidth - 128) / 2;
        textBox = new EditBox(font, boxLeft, height - 62, boxWidth, 20, Component.translatable("minecrafttweak.config.text"));
        textBox.setMaxLength(8000);
        textBox.setValue(pendingText != null ? pendingText : TextSettings.currentText(true));
        pendingText = null;
        addRenderableWidget(textBox);
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.import"), b -> applyText())
                .bounds(boxLeft + boxWidth + 4, height - 62, 64, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.copy"), b -> {
            if (minecraft != null) {
                minecraft.keyboardHandler.setClipboard(TextSettings.currentText(true));
                resultMessage = Component.translatable("minecrafttweak.config.copied").withStyle(ChatFormatting.GREEN);
            }
        }).bounds(boxLeft + boxWidth + 70, height - 62, 58, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.codes"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new CodeListScreen(this));
            }
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    private void keepTextAndRebuild() {
        pendingText = null;
        rebuildWidgets();
    }

    private void applyText() {
        TextSettings.Result result = TextSettings.apply(textBox.getValue(), true);
        if (!result.errors().isEmpty()) {
            resultMessage = result.errors().get(0).copy().withStyle(ChatFormatting.RED);
        } else {
            resultMessage = Component.translatable("minecrafttweak.config.applied", result.changed())
                    .withStyle(ChatFormatting.GREEN);
        }
        // 成功したときは今の設定で入力欄を作り直す。エラーのときは入力をそのまま残す。
        pendingText = result.errors().isEmpty() ? null : textBox.getValue();
        String keep = pendingText;
        rebuildWidgets();
        if (keep != null) {
            textBox.setValue(keep);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (textBox != null && textBox.isFocused() && (keyCode == 257 || keyCode == 335)) {
            applyText();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        for (Row row : rows) {
            if (row.option() == null) {
                Component label = Component.literal(row.feature().number() + ". ").withStyle(ChatFormatting.YELLOW)
                        .append(Component.translatable(row.feature().nameKey()).withStyle(ChatFormatting.WHITE));
                graphics.drawString(font, label, row.x(), row.y() + 6, 0xFFFFFF);
            } else {
                FeatureOption option = row.option();
                Component label = Component.literal(option.number() + " ").withStyle(ChatFormatting.GOLD)
                        .append(Component.translatable(option.nameKey()).withStyle(ChatFormatting.GRAY));
                graphics.drawString(font, label, row.x(), row.y() + 6, 0xFFFFFF);
                Component value = option.displayValue();
                graphics.drawString(font, value, row.x() + row.width() - font.width(value), row.y() + 6, 0xFFFFFF);
            }
        }
        if (rows.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.empty"),
                    width / 2, LIST_TOP + 10, 0xAAAAAA);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 90, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.text_help"),
                width / 2, height - 74, 0xAAAAAA);
        graphics.drawCenteredString(font, resultMessage, width / 2, height - 38, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
