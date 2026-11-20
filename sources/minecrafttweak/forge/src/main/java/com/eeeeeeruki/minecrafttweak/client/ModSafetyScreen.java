package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.client.feature.ModSafety;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** modの安全チェック（Modgrade）の結果の画面。問題のあるものから順に並べる。 */
public class ModSafetyScreen extends Screen {
    private static final int ROW = 22;
    private static final int TOP = 50;

    private final Screen parent;
    private int page;
    private int pageCount = 1;
    private final List<Row> rows = new ArrayList<>();

    private record Row(ModSafety.Entry entry, int y) {
    }

    public ModSafetyScreen(Screen parent) {
        super(Component.translatable("minecrafttweak.mod_safety.title"));
        this.parent = parent;
    }

    private static ChatFormatting color(ModSafety.Status s) {
        return switch (s) {
            case MALWARE -> ChatFormatting.RED;
            case WARNING -> ChatFormatting.GOLD;
            case PASSED -> ChatFormatting.GREEN;
            case PARTLY, NOT_CHECKED -> ChatFormatting.YELLOW;
            case UNKNOWN -> ChatFormatting.GRAY;
        };
    }

    private static Component statusText(ModSafety.Status s) {
        return Component.translatable("minecrafttweak.mod_safety.status." + s.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(color(s));
    }

    @Override
    protected void init() {
        rows.clear();
        List<ModSafety.Entry> all = ModSafety.results();
        int perPage = Math.max(1, (height - TOP - 60) / ROW);
        pageCount = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        int rowWidth = Math.min(420, width - 20);
        int left = (width - rowWidth) / 2;
        for (int i = page * perPage; i < Math.min(all.size(), (page + 1) * perPage); i++) {
            ModSafety.Entry e = all.get(i);
            int y = TOP + (i - page * perPage) * ROW;
            rows.add(new Row(e, y));
            if (!e.url().isEmpty()) {
                Button b = Button.builder(Component.translatable("minecrafttweak.mod_safety.page"), btn -> {
                            if (minecraft != null) {
                                minecraft.setScreen(new ConfirmLinkScreen(open -> {
                                    if (open) {
                                        Util.getPlatform().openUri(e.url());
                                    }
                                    minecraft.setScreen(this);
                                }, e.url(), true));
                            }
                        }).bounds(left + rowWidth - 56, y, 56, 20)
                        .tooltip(Tooltip.create(Component.literal(e.url())))
                        .build();
                addRenderableWidget(b);
            }
        }
        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 60, height - 52, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 40, height - 52, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }
        Button again = Button.builder(Component.translatable("minecrafttweak.mod_safety.recheck"), b -> {
            ModSafety.checkNow();
            rebuildWidgets();
        }).bounds(width / 2 - 154, height - 26, 150, 20).build();
        again.active = ModSafety.state() != ModSafety.State.CHECKING;
        addRenderableWidget(again);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 + 4, height - 26, 150, 20).build());
    }

    @Override
    public void tick() {
        super.tick();
        // 調べ終わったら作り直す
        if (ModSafety.state() != ModSafety.State.CHECKING && rows.isEmpty() && !ModSafety.results().isEmpty()) {
            rebuildWidgets();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        Component summary;
        if (ModSafety.state() == ModSafety.State.CHECKING) {
            summary = Component.translatable("minecrafttweak.mod_safety.checking");
        } else if (ModSafety.state() == ModSafety.State.FAILED) {
            summary = Component.translatable("minecrafttweak.mod_safety.failed", ModSafety.error()).withStyle(ChatFormatting.RED);
        } else if (ModSafety.state() == ModSafety.State.IDLE) {
            summary = Component.translatable("minecrafttweak.mod_safety.line.idle_screen");
        } else {
            summary = Component.translatable("minecrafttweak.mod_safety.summary",
                    ModSafety.count(ModSafety.Status.MALWARE), ModSafety.count(ModSafety.Status.WARNING),
                    ModSafety.count(ModSafety.Status.PASSED), ModSafety.count(ModSafety.Status.PARTLY) + ModSafety.count(ModSafety.Status.NOT_CHECKED),
                    ModSafety.count(ModSafety.Status.UNKNOWN));
        }
        graphics.drawCenteredString(font, summary, width / 2, 24, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("minecrafttweak.mod_safety.notice").withStyle(ChatFormatting.DARK_GRAY),
                width / 2, 36, 0xFFFFFF);
        int rowWidth = Math.min(420, width - 20);
        int left = (width - rowWidth) / 2;
        for (Row r : rows) {
            ModSafety.Entry e = r.entry();
            Component status = statusText(e.status());
            graphics.drawString(font, status, left, r.y() + 6, 0xFFFFFF);
            int x = left + 90;
            String name = e.title().isEmpty() ? e.fileName() : e.title() + "  (" + e.fileName() + ")";
            int max = rowWidth - 90 - 62;
            if (font.width(name) > max) {
                name = font.plainSubstrByWidth(name, max - font.width("...")) + "...";
            }
            graphics.drawString(font, Component.literal(name), x, r.y() + 2, 0xFFFFFF);
            if (!e.reasons().isEmpty()) {
                graphics.drawString(font, Component.literal(String.join(", ", e.reasons())).withStyle(ChatFormatting.GRAY),
                        x, r.y() + 12, 0xFFFFFF);
            }
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 46, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
