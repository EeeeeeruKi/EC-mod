package com.eeeeeeruki.minecrafttweak.feature.client;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * 英語でも検索。いまの言語の名前に加えて、英語の名前でもアイテムを探せるようにする。
 * クリエイティブのインベントリ・レシピ本・JEI・EMI・AE2 の端末の検索に対応。
 */
public class EnglishSearchFeature extends Feature {
    public static EnglishSearchFeature INSTANCE;

    public EnglishSearchFeature() {
        super("english_search", FeatureCategory.CLIENT,
                "Lets you search items by their English names as well as the names in your language (creative inventory, recipe book, JEI, EMI, AE2 terminals).");
        INSTANCE = this;
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.EnglishNames.register(modBus);
        com.eeeeeeruki.minecrafttweak.client.feature.EnglishSearch.register();
    }
}
