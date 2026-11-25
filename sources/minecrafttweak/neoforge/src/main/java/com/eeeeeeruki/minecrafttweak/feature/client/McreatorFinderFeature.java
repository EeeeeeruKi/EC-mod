package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.neoforged.bus.api.IEventBus;

/**
 * MCreator製modさがし。コマンド /mcreator で、入っているmodの中から MCreator で作られたものを探してチャットに送る。
 * /mcreator check は、チャットに送らず自分の画面にだけ出す。
 */
public class McreatorFinderFeature extends Feature {
    public static McreatorFinderFeature INSTANCE;

    public McreatorFinderFeature() {
        super("mcreator_finder", FeatureCategory.CLIENT,
                "Adds /mcreator: finds the installed mods made with MCreator and sends the list to chat (/mcreator check shows it only to you).");
        INSTANCE = this;
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.McreatorFinder.register();
    }
}
