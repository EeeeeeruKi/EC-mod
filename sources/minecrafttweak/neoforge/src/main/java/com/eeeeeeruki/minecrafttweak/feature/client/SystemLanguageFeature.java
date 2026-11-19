package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 言語の画面に「システムの言語」を足す。選ぶと、パソコンの言語に合わせて言語が切り替わる。
 * （この機能は最初からオン）
 */
public class SystemLanguageFeature extends Feature {
    public static SystemLanguageFeature INSTANCE;
    public ModConfigSpec.BooleanValue follow;

    public SystemLanguageFeature() {
        super("system_language", FeatureCategory.CLIENT, true,
                "Adds \"System language\" to the language screen, which follows the language of your computer.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        follow = boolOption(builder, "follow", "Follow the system language (set from the language screen).", false);
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.SystemLanguage.register();
    }
}
