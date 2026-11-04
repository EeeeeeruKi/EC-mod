package com.eeeeeeruki.tonsofadv;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

/** MOD一覧の「設定」ボタンから開く、小さな設定画面 */
final class ClientSetup {
    private ClientSetup() {
    }

    static void init() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new SettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.addListener(ClientSetup::onPlaySound);
    }

    /** エーテルの進捗トーストの音（「音楽」で鳴る）を「ジュークボックス/音ブロック」の音量で鳴らし直す */
    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s == null || s.getSource() != SoundSource.MUSIC || !Config.soundsAsRecords()) {
            return;
        }
        ResourceLocation id = s.getLocation();
        if ("aether".equals(id.getNamespace()) && id.getPath().startsWith("ui.toast.")) {
            event.setSound(new SimpleSoundInstance(id, SoundSource.RECORDS, 1.0F, 1.0F,
                    SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, 0.0D, 0.0D, 0.0D, true));
        }
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
                    .create(width / 2 - 100, height / 2 - 36, 200, 20, Component.translatable("tonsofadv.configuration.specialSounds"),
                            (button, value) -> {
                                Config.SPECIAL_SOUNDS.set(value);
                                Config.SPEC.save();
                            }));
            addRenderableWidget(CycleButton.onOffBuilder(Config.soundsAsRecords())
                    .withTooltip(v -> Tooltip.create(Component.translatable("tonsofadv.configuration.soundsAsRecords.tooltip")))
                    .create(width / 2 - 100, height / 2 - 12, 200, 20, Component.translatable("tonsofadv.configuration.soundsAsRecords"),
                            (button, value) -> {
                                Config.SOUNDS_AS_RECORDS.set(value);
                                Config.SPEC.save();
                            }));
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                    .bounds(width / 2 - 100, height / 2 + 20, 200, 20).build());
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            renderBackground(g);
            g.drawCenteredString(font, title, width / 2, height / 2 - 72, 0xFFFFFF);
            g.drawCenteredString(font, Component.translatable("tonsofadv.configuration.specialSounds.tooltip"), width / 2, height / 2 - 56, 0xA0A0A0);
            super.render(g, mouseX, mouseY, partialTick);
        }

        @Override
        public void onClose() {
            minecraft.setScreen(parent);
        }
    }
}
