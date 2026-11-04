package com.eeeeeeruki.tonsofadv;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** MOD一覧の「設定」ボタンから NeoForge の設定画面を開けるようにします */
final class ClientSetup {
    private ClientSetup() {
    }

    static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        NeoForge.EVENT_BUS.addListener(ClientSetup::onPlaySound);
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
}
