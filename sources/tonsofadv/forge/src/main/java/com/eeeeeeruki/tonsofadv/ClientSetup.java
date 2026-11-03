package com.eeeeeeruki.tonsofadv;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

/** MOD一覧の「設定」ボタンから開く、小さな設定画面 */
final class ClientSetup {
    private ClientSetup() {
    }

    static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
    }

    private static final class SettingsScreen extends Screen {
        private final Screen parent;

        SettingsScreen(Screen parent) {
            super(Component.translatable("tonsofadv.configuration.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            addRenderableWidget(CycleButton.onOffBuilder(Config.specialSounds())
                    .create(width / 2 - 100, height / 2 - 24, 200, 20, Component.translatable("tonsofadv.configuration.specialSounds"),
                            (button, value) -> {
                                Config.SPECIAL_SOUNDS.set(value);
                                Config.SPEC.save();
                            }));
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 100, height / 2 + 8, 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderBackground(g);
            g.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFF);
            g.drawCenteredString(font, Component.translatable("tonsofadv.configuration.specialSounds.tooltip"), width / 2, height / 2 - 44, 0xA0A0A0);
            super.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }
}
