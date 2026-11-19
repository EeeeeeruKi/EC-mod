package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;


/**
 * 設定コードの画面（ホームの「設定コード」から開く）。
 * 下の欄に設定コード（例: 1-t,1a-0,v1-12,s3-f）を貼って「インポート」を押すと、まとめて変更できる。
 * 「MODのコード」「描画のコード」で、初期値と違う設定だけをコードにしてクリップボードに写す。
 */
public class CodeScreen extends Screen {

    private final Screen parent;
    private EditBox textBox;
    private String pendingText;
    private Component resultMessage = Component.empty();

    public CodeScreen(Screen parent) {
        super(Component.translatable("minecrafttweak.config.code_screen"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 設定コード欄
        int boxWidth = Math.min(250, width - 90);
        int boxLeft = (width - boxWidth - 68) / 2;
        textBox = new EditBox(font, boxLeft, height / 2 - 20, boxWidth, 20, Component.translatable("minecrafttweak.config.text"));
        textBox.setMaxLength(32000);
        textBox.setHint(Component.translatable("minecrafttweak.config.text_hint").withStyle(ChatFormatting.DARK_GRAY));
        textBox.setValue(pendingText != null ? pendingText : "");
        pendingText = null;
        addRenderableWidget(textBox);
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.import"), b -> applyText())
                .bounds(boxLeft + boxWidth + 4, height / 2 - 20, 64, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.import.tooltip")))
                .build());

        // コピーと切り替え
        int w = Math.min(78, (width - 20) / 4);
        int left = width / 2 - w * 2;
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.copy_mod"), b -> copy(TextSettings.Group.MOD))
                .bounds(left, height / 2 + 20, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.copy_mod.tooltip")))
                .build());
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.copy_client"), b -> copy(TextSettings.Group.CLIENT))
                .bounds(left + w, height / 2 + 20, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.copy_client.tooltip")))
                .build());
        addRenderableWidget(Button.builder(toggleText("minecrafttweak.config.compress", compress), b -> {
                    compress = !compress;
                    b.setMessage(toggleText("minecrafttweak.config.compress", compress));
                }).bounds(left + w * 2, height / 2 + 20, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.compress.tooltip")))
                .build());
        addRenderableWidget(Button.builder(toggleText("minecrafttweak.config.reset_first", resetFirst), b -> {
                    resetFirst = !resetFirst;
                    b.setMessage(toggleText("minecrafttweak.config.reset_first", resetFirst));
                }).bounds(left + w * 3, height / 2 + 20, w - 2, 20)
                .tooltip(Tooltip.create(Component.translatable("minecrafttweak.config.reset_first.tooltip")))
                .build());

        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.config.codes"), b -> {
            if (minecraft != null) {
                minecraft.setScreen(new CodeListScreen(this));
            }
        }).bounds(width / 2 - 100, height - 56, 200, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 30, 200, 20).build());
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
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.config.text_help"),
                width / 2, height / 2 - 34, 0xAAAAAA);
        graphics.drawCenteredString(font, resultMessage, width / 2, height / 2 + 6, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
