package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.client.feature.ChatImages;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.nio.file.Path;

/** チャットの「＋」から開く、画像を添付する画面。閉じるとチャットに戻る（書きかけの文字はそのまま）。 */
public class ChatAttachScreen extends Screen {
    private final String chatText;
    private EditBox link;
    private Component message = Component.empty();
    private int messageColor = 0xFF7070;

    public ChatAttachScreen(String chatText) {
        super(Component.translatable("minecrafttweak.chat_images.attach.title"));
        this.chatText = chatText == null ? "" : chatText;
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        int y = height / 2 - 62;

        Button file = Button.builder(Component.translatable("minecrafttweak.chat_images.attach.file"), b -> pickFile())
                .bounds(x, y, 200, 20).build();
        if (!ChatImages.serverSupportsFiles()) {
            file.active = false;
            file.setTooltip(Tooltip.create(Component.translatable("minecrafttweak.chat_images.attach.file_unavailable")));
        }
        addRenderableWidget(file);

        link = new EditBox(font, x, y + 34, 200, 20, Component.translatable("minecrafttweak.chat_images.attach.link_hint"));
        link.setMaxLength(256);
        link.setHint(Component.translatable("minecrafttweak.chat_images.attach.link_hint").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(link);
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.chat_images.attach.link"), b -> addLink())
                .bounds(x, y + 56, 200, 20).build());

        Button remove = Button.builder(Component.translatable("minecrafttweak.chat_images.attach.remove"), b -> {
            ChatImages.clearPending();
            back(chatText);
        }).bounds(x, y + 88, 98, 20).build();
        remove.active = ChatImages.hasPending();
        addRenderableWidget(remove);
        addRenderableWidget(Button.builder(Component.translatable("minecrafttweak.chat_images.attach.back"), b -> back(chatText))
                .bounds(x + 102, y + 88, 98, 20).build());
    }

    private void pickFile() {
        File pictures = new File(System.getProperty("user.home"), "Pictures");
        String start = (pictures.isDirectory() ? pictures.getAbsolutePath() : System.getProperty("user.home")) + File.separator;
        FilePicker.openIn(ChatImages.FILE_PATTERNS, start, picked -> {
            Path path = Path.of(picked);
            String error = ChatImages.checkFile(path);
            if (error != null) {
                message = Component.translatable(error, ChatImages.maxUploadBytes() / 1024);
                messageColor = 0xFF7070;
                return;
            }
            ChatImages.setPendingFile(path);
            if (minecraft != null && minecraft.screen == this) {
                back(chatText);
            }
        });
    }

    private void addLink() {
        String url = link.getValue().strip();
        if (!url.matches("(?i)https?://\\S+")) {
            message = Component.translatable("minecrafttweak.chat_images.error.bad_link");
            messageColor = 0xFF7070;
            return;
        }
        if (!ChatImages.serverSupportsFiles()) {
            // サーバーにこのmodがないときは、前と同じようにリンクをチャットの文に入れる
            String text = chatText.isBlank() ? url : chatText.stripTrailing() + " " + url;
            if (text.length() > 256) {
                message = Component.translatable("minecrafttweak.chat_images.error.too_long");
                messageColor = 0xFF7070;
                return;
            }
            back(text);
            return;
        }
        // 画像ファイルと同じように添付する（送るときに読み込んで、サーバーへ送る）
        ChatImages.setPendingUrl(url);
        back(chatText);
    }

    private void back(String text) {
        if (minecraft != null) {
            minecraft.setScreen(new ChatScreen(text));
        }
    }

    @Override
    public void onClose() {
        back(chatText);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        int y = height / 2 - 62;
        graphics.drawCenteredString(font, title, width / 2, y - 24, 0xFFFFFF);
        graphics.drawString(font, Component.translatable("minecrafttweak.chat_images.attach.link_label"), width / 2 - 100, y + 24, 0xA0A0A0);
        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.chat_images.attach.note"), width / 2, y + 114, 0x808080);
        if (!message.getString().isEmpty()) {
            graphics.drawCenteredString(font, message, width / 2, y + 128, messageColor);
        }
    }
}
