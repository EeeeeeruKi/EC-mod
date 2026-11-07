package com.eeeeeeruki.compacttoasts;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

public class ToastsConfigScreen extends Screen {
    @Nullable
    private final Screen parent;
    private int max;
    private int threshold;
    private HeaderMode headerMode;
    private IntSlider thresholdSlider;

    public ToastsConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("compacttoasts.config.title"));
        this.parent = parent;
        this.max = ToastsConfig.maxToasts();
        this.threshold = ToastsConfig.compactThreshold();
        this.headerMode = ToastsConfig.headerMode();
    }

    @Override
    protected void init() {
        int x = this.width / 2 - 100;
        int y = this.height / 4;
        this.addRenderableWidget(new IntSlider(x, y, 200, 20, 1, ToastsConfig.MAX_LIMIT, this.max,
                v -> Component.translatable("compacttoasts.config.maxToasts", v),
                v -> {
                    this.max = v;
                    if (this.thresholdSlider != null) this.thresholdSlider.refresh();
                }));
        this.thresholdSlider = this.addRenderableWidget(new IntSlider(x, y + 40, 200, 20, 1, ToastsConfig.MAX_LIMIT + 1, this.threshold,
                v -> v > this.max
                        ? Component.translatable("compacttoasts.config.compactOff")
                        : Component.translatable("compacttoasts.config.compactThreshold", v),
                v -> this.threshold = v));
        this.addRenderableWidget(Button.builder(headerLabel(), b -> {
            this.headerMode = this.headerMode.next();
            b.setMessage(headerLabel());
        }).bounds(x, y + 80, 200, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("compacttoasts.config.test"), b -> {
            this.apply();
            for (int i = 1; i <= 6; i++) {
                SystemToast.add(this.minecraft.getToasts(), SystemToast.SystemToastId.WORLD_BACKUP,
                        Component.translatable("compacttoasts.test.title", i),
                        Component.translatable("compacttoasts.test.message"));
            }
        }).bounds(x, y + 124, 200, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
                .bounds(x, y + 150, 200, 20).build());
    }

    private Component headerLabel() {
        return Component.translatable("compacttoasts.config.header", Component.translatable(this.headerMode.key()));
    }

    private void apply() {
        ToastsConfig.save(this.max, this.threshold, this.headerMode);
    }

    @Override
    public void onClose() {
        this.apply();
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        
        super.render(g, mouseX, mouseY, partialTick);
        int y = this.height / 4;
        g.drawCenteredString(this.font, this.title, this.width / 2, y - 24, 0xFFFFFF);
        g.drawCenteredString(this.font, Component.translatable("compacttoasts.config.hint1"), this.width / 2, y + 23, 0xA0A0A0);
        g.drawCenteredString(this.font, Component.translatable("compacttoasts.config.hint2"), this.width / 2, y + 63, 0xA0A0A0);
        g.drawCenteredString(this.font, Component.translatable("compacttoasts.config.hint3"), this.width / 2, y + 103, 0xA0A0A0);
    }

    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntFunction<Component> label;
        private final IntConsumer onChange;

        IntSlider(int x, int y, int w, int h, int min, int max, int current, IntFunction<Component> label, IntConsumer onChange) {
            super(x, y, w, h, Component.empty(), (Math.max(min, Math.min(max, current)) - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.label = label;
            this.onChange = onChange;
            this.updateMessage();
        }

        int current() {
            return this.min + (int) Math.round(this.value * (this.max - this.min));
        }

        void refresh() {
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (this.label != null) this.setMessage(this.label.apply(this.current()));
        }

        @Override
        protected void applyValue() {
            this.onChange.accept(this.current());
        }
    }
}
