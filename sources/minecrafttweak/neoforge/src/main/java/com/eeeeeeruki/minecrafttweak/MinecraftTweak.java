package com.eeeeeeruki.minecrafttweak;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TweakConfig;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * minEcraft twEEk のメインクラス。
 * 機能（Feature）は core/Features.java に登録し、分類ごとに設定でオン/オフできる。
 */
@Mod(MinecraftTweak.MODID)
public class MinecraftTweak {
    public static final String MODID = "minecrafttweak";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftTweak(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TweakConfig.SPEC, MODID + ".toml");

        for (Feature feature : Features.all()) {
            feature.register(modBus);
        }

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.eeeeeeruki.minecrafttweak.client.ClientSetup.init(container);
        }

        LOGGER.info("minEcraft twEEk: {} feature(s) loaded", Features.all().size());
    }
}
