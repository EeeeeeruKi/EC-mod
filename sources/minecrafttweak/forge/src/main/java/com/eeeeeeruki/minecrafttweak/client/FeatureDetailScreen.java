package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureOption;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1つの機能の詳細設定画面。
 * オン/オフの設定はボタン、数値の設定は入力欄で変える。「完了」で保存して戻る。
 */
public class FeatureDetailScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private final Feature feature;
    private final Map<FeatureOption, EditBox> boxes = new LinkedHashMap<>();
    private final Map<FeatureOption, String> pendingText = new LinkedHashMap<>();
    private final List<RowLabel> labels = new ArrayList<>();
    private List<FormattedCharSequence> description = List.of();
    private Component message = Component.empty();
    private int page;
    private int pageCount = 1;
    private int listTop;

    private record RowLabel(Component text, int x, int y) {
    }

    public FeatureDetailScreen(Screen parent, Feature feature) {
        super(Component.translatable("minecrafttweak.config.details.title",
                Component.translatable(feature.nameKey())));
        this.parent = parent;
        this.feature = feature;
    }

    @Override
    protected void init() {
        // 入力途中の値を、作り直しても残す
        for (Map.Entry<FeatureOption, EditBox> e : boxes.entrySet()) {
            pendingText.put(e.getKey(), e.getValue().getValue());
        }
        boxes.clear();
        labels.clear();

        int rowWidth = Math.min(320, width - 40);
        int left = (width - rowWidth) / 2;
        description = font.split(Component.translatable(feature.descriptionKey()), rowWidth);
        if (description.size() > 3) {
            description = description.subList(0, 3);
        }
        int y = 30 + description.size() * 10 + 6;

        // 機能そのもののオン/オフ
        labels.add(new RowLabel(Component.literal(feature.number() + " ").withStyle(ChatFormatting.YELLOW)
                .append(Component.translatable("minecrafttweak.config.details.enabled").withStyle(ChatFormatting.WHITE)),
                left, y + 6));
        addRenderableWidget(Button.builder(TextSettings.onOff(feature.isEnabled()), b -> {
            feature.setEnabled(!feature.isEnabled());
            rebuildWidgets();
        }).bounds(left + rowWidth - 80, y, 80, 20).build());
        listTop = y + ROW_HEIGHT + 4;

        List<FeatureOption> options = feature.options();
        int listBottom = height - 52;
        int perPage = Math.max(1, (listBottom - listTop) / ROW_HEIGHT);
        pageCount = Math.max(1, (options.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int start = page * perPage;
        int end = Math.min(options.size(), start + perPage);

        for (int i = start; i < end; i++) {
            FeatureOption option = options.get(i);
            int rowY = listTop + (i - start) * ROW_HEIGHT;
            labels.add(new RowLabel(Component.literal(option.number() + " ").withStyle(ChatFormatting.GOLD)
                    .append(Component.translatable(option.nameKey()).withStyle(ChatFormatting.WHITE)),
                    left, rowY + 6));
            Tooltip tooltip = Tooltip.create(I18n.exists(option.descriptionKey())
                    ? Component.translatable(option.descriptionKey()).append("\n").append(rangeLine(option))
                    : rangeLine(option));

            if (option.type() == FeatureOption.Type.BOOLEAN) {
                addRenderableWidget(Button.builder(TextSettings.onOff(option.booleanValue()), b -> {
                    option.apply(option.booleanValue() ? "f" : "t");
                    rebuildWidgets();
                }).bounds(left + rowWidth - 80, rowY, 80, 20).tooltip(tooltip).build());
            } else {
                boolean text = option.type() == FeatureOption.Type.STRING;
                int labelWidth = font.width(labels.get(labels.size() - 1).text());
                int boxWidth = text ? Math.max(120, rowWidth - labelWidth - 10) : 78;
                EditBox box = new EditBox(font, left + rowWidth - boxWidth - 1, rowY + 1, boxWidth, 18,
                        Component.translatable(option.nameKey()));
                box.setMaxLength(text ? 2048 : 32);
                box.setValue(pendingText.getOrDefault(option, option.editText()));
                box.setTooltip(tooltip);
                addRenderableWidget(box);
                boxes.put(option, box);
            }
        }
        pendingText.clear();

        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                saveBoxes();
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 60, height - 50, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> {
                saveBoxes();
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 40, height - 50, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }

        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.details.reset"), b -> {
            for (FeatureOption option : feature.options()) {
                option.reset();
            }
            boxes.clear();
            message = Component.translatable("minecrafttweak.config.details.reset_done").withStyle(ChatFormatting.GREEN);
            rebuildWidgets();
        }).bounds(width / 2 - 154, height - 26, 150, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
            if (saveBoxes()) {
                onClose();
            }
        }).bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    private static Component rangeLine(FeatureOption option) {
        if (option.type() == FeatureOption.Type.STRING) {
            return Component.translatable("minecrafttweak.config.details.text").withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("minecrafttweak.config.details.range", option.rangeText())
                .withStyle(ChatFormatting.GRAY);
    }

    /** 入力欄の値を保存する。全部うまくいけば true。 */
    private boolean saveBoxes() {
        Component firstError = null;
        for (Map.Entry<FeatureOption, EditBox> e : boxes.entrySet()) {
            String text = e.getValue().getValue().trim();
            if (text.equals(e.getKey().editText())) {
                continue;
            }
            Component error = e.getKey().applyEdit(text);
            if (error != null && firstError == null) {
                firstError = error;
            }
        }
        if (firstError != null) {
            message = firstError.copy().withStyle(ChatFormatting.RED);
            return false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            for (EditBox box : boxes.values()) {
                if (box.isFocused()) {
                    if (saveBoxes()) {
                        message = Component.translatable("minecrafttweak.config.details.saved").withStyle(ChatFormatting.GREEN);
                        boxes.clear();
                        rebuildWidgets();
                    }
                    return true;
                }
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        int y = 30;
        for (FormattedCharSequence line : description) {
            graphics.drawCenteredString(font, line, width / 2, y, 0xAAAAAA);
            y += 10;
        }
        for (RowLabel label : labels) {
            graphics.drawString(font, label.text(), label.x(), label.y(), 0xFFFFFF);
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 44, 0xFFFFFF);
        }
        graphics.drawCenteredString(font, message, width / 2, height - 38 - (pageCount > 1 ? 24 : 0), 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
