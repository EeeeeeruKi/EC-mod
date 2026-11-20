package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.neoforged.bus.api.IEventBus;

/**
 * modの安全チェック。Modgrade API（https://modgrade.com/api）で、mods フォルダのmodを調べる。
 * 調べるのは、初めて起動したとき・modを足した／更新した／消したとき・前の起動でゲームがクラッシュしたとき。
 */
public class ModSafetyFeature extends Feature {
    public static ModSafetyFeature INSTANCE;

    public ModSafetyFeature() {
        super("mod_safety", FeatureCategory.CLIENT,
                "Checks your mods with the Modgrade API on the first start, when mods are added, updated or removed, and after a crash.");
        INSTANCE = this;
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.ModSafety.register(modBus);
    }
}
