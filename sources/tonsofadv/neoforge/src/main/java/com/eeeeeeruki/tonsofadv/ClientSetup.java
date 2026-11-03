package com.eeeeeeruki.tonsofadv;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** MOD一覧の「設定」ボタンから NeoForge の設定画面を開けるようにします */
final class ClientSetup {
    private ClientSetup() {
    }

    static void init(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
