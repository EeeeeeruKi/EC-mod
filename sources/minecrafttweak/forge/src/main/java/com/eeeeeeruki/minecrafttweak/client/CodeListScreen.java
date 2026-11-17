package com.eeeeeeruki.minecrafttweak.client;

import com.eeeeeeruki.minecrafttweak.core.CodeEntry;
import com.eeeeeeruki.minecrafttweak.core.CodeProvider;
import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureOption;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** 設定コードで使える項目番号の一覧。タブで「このMOD / バニラ / Sodium / Sodium Extra」を切り替える。 */
public class CodeListScreen extends Screen {
    private static final int LINE_HEIGHT = 12;
    private static final int TOP = 56;

    private final Screen parent;
    private int tab;
    private int page;
    private int pageCount = 1;
    private final List<Component[]> lines = new ArrayList<>(); // {番号と名前, 値}
    private Component note = Component.empty();

    public CodeListScreen(Screen parent) {
        super(Component.translatable("minecrafttweak.codes.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        List<CodeProvider> providers = TextSettings.providers();
        int tabCount = providers.size() + 1;
        int tabWidth = Math.min(110, (width - 20) / tabCount);
        int tabLeft = (width - tabWidth * tabCount) / 2;
        for (int i = 0; i < tabCount; i++) {
            int index = i;
            Component label = i == 0 ? Component.translatable("minecrafttweak.codes.tab.mod") : providers.get(i - 1).displayName();
            Button b = Button.builder(label, btn -> {
                tab = index;
                page = 0;
                rebuildWidgets();
            }).bounds(tabLeft + i * tabWidth, 28, tabWidth - 2, 20).build();
            b.active = i != tab;
            addRenderableWidget(b);
        }

        List<Component[]> all = new ArrayList<>();
        note = Component.empty();
        if (tab == 0) {
            for (Feature feature : Features.all()) {
                all.add(new Component[]{
                        Component.literal(feature.number() + " ").withStyle(ChatFormatting.YELLOW)
                                .append(Component.translatable(feature.nameKey()).withStyle(ChatFormatting.WHITE)),
                        TextSettings.onOff(feature.isEnabled())});
                for (FeatureOption option : feature.options()) {
                    all.add(new Component[]{
                            Component.literal("  " + option.number() + " ").withStyle(ChatFormatting.GOLD)
                                    .append(Component.translatable(option.nameKey()).withStyle(ChatFormatting.GRAY)),
                            option.displayValue()});
                }
            }
        } else {
            CodeProvider provider = providers.get(tab - 1);
            boolean installed = provider.available();
            if (!installed) {
                note = Component.translatable("minecrafttweak.codes.not_installed", provider.displayName())
                        .withStyle(ChatFormatting.GRAY);
            }
            for (CodeEntry entry : provider.entries()) {
                boolean usable;
                String value = null;
                try {
                    usable = installed && entry.available();
                    if (usable) {
                        value = entry.textValue();
                    }
                } catch (Throwable t) {
                    usable = false;
                }
                Component name;
                try {
                    name = entry.name();
                } catch (Throwable t) {
                    name = Component.literal(entry.key());
                }
                all.add(new Component[]{
                        Component.literal(entry.key() + " ").withStyle(usable ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)
                                .append(name.copy().withStyle(usable ? ChatFormatting.WHITE : ChatFormatting.DARK_GRAY)),
                        usable ? Component.literal(String.valueOf(value)).withStyle(ChatFormatting.AQUA)
                                : Component.translatable("minecrafttweak.codes.unavailable").withStyle(ChatFormatting.DARK_GRAY)});
            }
        }

        int perPage = Math.max(1, (height - 40 - TOP) / LINE_HEIGHT - (note.getString().isEmpty() ? 0 : 1));
        pageCount = Math.max(1, (all.size() + perPage - 1) / perPage);
        page = Math.max(0, Math.min(page, pageCount - 1));
        lines.clear();
        lines.addAll(all.subList(page * perPage, Math.min(all.size(), (page + 1) * perPage)));

        if (pageCount > 1) {
            Button prev = Button.builder(Component.literal("<"), b -> {
                page--;
                rebuildWidgets();
            }).bounds(width / 2 - 126, height - 26, 20, 20).build();
            prev.active = page > 0;
            addRenderableWidget(prev);
            Button next = Button.builder(Component.literal(">"), b -> {
                page++;
                rebuildWidgets();
            }).bounds(width / 2 + 106, height - 26, 20, 20).build();
            next.active = page < pageCount - 1;
            addRenderableWidget(next);
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, height - 26, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        int rowWidth = Math.min(360, width - 40);
        int left = (width - rowWidth) / 2;
        int y = TOP;
        if (!note.getString().isEmpty()) {
            graphics.drawCenteredString(font, note, width / 2, TOP - 4, 0xAAAAAA);
            y += LINE_HEIGHT;
        }
        for (Component[] line : lines) {
            graphics.drawString(font, line[0], left, y, 0xFFFFFF);
            graphics.drawString(font, line[1], left + rowWidth - font.width(line[1]), y, 0xFFFFFF);
            y += LINE_HEIGHT;
        }
        if (pageCount > 1) {
            graphics.drawCenteredString(font, (page + 1) + " / " + pageCount, width / 2, height - 38, 0xAAAAAA);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}
