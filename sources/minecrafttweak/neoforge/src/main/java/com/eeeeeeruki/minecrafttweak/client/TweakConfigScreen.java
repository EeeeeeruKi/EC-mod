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
 * 「MODのコード」「描画のコード」で、初期値と違う設定だけをコードにしてクリップボードに写す。
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

        int listBottom = height - 124;
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
            }).bounds(width / 2 - 60, height - 122, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);

            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                keepTextAndRebuild();
            }).bounds(width / 2 + 40, height - 122, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }

        // 設定コード欄
        int boxWidth = Math.min(250, width - 90);
        int boxLeft = (width - boxWidth - 68) / 2;
        textBox = new EditBox(font, boxLeft, height - 86, boxWidth, 20, Component.translatable("minecrafttweak.config.text"));
        textBox.setMaxLength(32000);
        textBox.setHint(Component.translatable("minecrafttweak.config.text_hint").withStyle(ChatFormatting.DARK_GRAY));
        textBox.setValue(pendingText != null ? pendingText : "");
        pendingText = null;
        addRenderableWidget(textBox);
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.import"), b -> applyText())
                .bounds(boxLeft + boxWidth + 4, height - 86, 64, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.import.tooltip")))
                .build());

        // コピーと切り替え
        int w = Math.min(78, (width - 20) / 4);
        int left = width / 2 - w * 2;
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.copy_mod"), b -> copy(TextSettings.Group.MOD))
                .bounds(left, height - 50, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.copy_mod.tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.copy_client"), b -> copy(TextSettings.Group.CLIENT))
                .bounds(left + w, height - 50, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.copy_client.tooltip")))
                .build());
        addRenderableWidget(Button.builder(toggleText("minecrafttweak.config.compress", compress), b -> {
                    compress = !compress;
                    b.setMessage(toggleText("minecrafttweak.config.compress", compress));
                }).bounds(left + w * 2, height - 50, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.compress.tooltip")))
                .build());
        addRenderableWidget(Button.builder(toggleText("minecrafttweak.config.reset_first", resetFirst), b -> {
                    resetFirst = !resetFirst;
                    b.setMessage(toggleText("minecrafttweak.config.reset_first", resetFirst));
                }).bounds(left + w * 3, height - 50, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.reset_first.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.codes"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new CodeListScreen(this));
            }
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    /** 圧縮するか / インポートの前に初期値に戻すか（ゲームを閉じるまで覚えておく）。 */
    private static boolean compress = false;
    private static boolean resetFirst = false;

    private static Component toggleText(String key, boolean value) {
        return Component.translatable(key).append(": ").append(TextSettings.onOff(value));
    }

    private void copy(TextSettings.Group group) {
        if (minecraft == null) {
            return;
        }
        String code = TextSettings.currentText(group, true);
        if (code.isEmpty()) {
            resultMessage = Component.translatable("minecrafttweak.config.copied_empty").withStyle(ChatFormatting.YELLOW);
            return;
        }
        boolean shorterRaw = false;
        if (compress) {
            String packed = com.eeeeeeruki.minecrafttweak.core.CodeCompression.encode(code);
            if (packed.length() < code.length()) {
                code = packed;
            } else {
                shorterRaw = true; // 短いコードは圧縮すると逆に長くなるので、そのまま
            }
        }
        minecraft.keyboardHandler.setClipboard(code);
        resultMessage = Component.translatable(group == TextSettings.Group.MOD
                        ? "minecrafttweak.config.copied_mod" : "minecrafttweak.config.copied_client", code.length())
                .withStyle(ChatFormatting.GREEN);
        if (shorterRaw) {
            resultMessage = resultMessage.copy().append(Component.translatable("minecrafttweak.config.copied_raw"));
        }
    }

    private void keepTextAndRebuild() {
        pendingText = textBox != null ? textBox.getValue() : null;
        rebuildWidgets();
    }

    private void applyText() {
        TextSettings.Result result = TextSettings.apply(textBox.getValue(), true, resetFirst);
        if (!result.errors().isEmpty()) {
            resultMessage = result.errors().get(0).copy().withStyle(ChatFormatting.RED);
        } else {
            resultMessage = Component.translatable("minecrafttweak.config.applied", result.changed())
                    .withStyle(ChatFormatting.GREEN);
        }
        // 成功したときは入力欄を空にする。エラーのときは入力をそのまま残す。
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
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 116, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.text_help"),
                width / 2, height - 98, 0xAAAAAA);
        graphics.drawCenteredString(font, resultMessage, width / 2, height - 62, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
