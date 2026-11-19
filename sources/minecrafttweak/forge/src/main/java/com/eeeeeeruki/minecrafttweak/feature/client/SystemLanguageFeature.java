package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * 言語の画面の一覧のいちばん上に「システムの言語」を足す。選ぶと、パソコンの言語に合わせて言語が切り替わる。
 * （この機能は最初からオン）
 */
public class SystemLanguageFeature extends Feature {
    public static SystemLanguageFeature INSTANCE;

    public SystemLanguageFeature() {
        super("system_language", FeatureCategory.CLIENT, true,
                "Adds \"System language\" to the top of the language list, which follows the language of your computer.");
        INSTANCE = this;
    }


    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.SystemLanguage.register();
    }
}
