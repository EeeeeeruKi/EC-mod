package com.eeeeeeruki.minecrafttweak;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TweakConfig;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.eeeeeeruki.minecrafttweak.compat.QuarkCodes;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import com.eeeeeeruki.minecrafttweak.command.TweakCommand;
import org.slf4j.Logger;

/**
 * minEcraft twEEk のメインクラス。
 * Quark の前提MOD。Quark にない改善機能だけを入れる。
 * 機能（Feature）は core/Features.java に登録し、設定画面・テキスト・コマンドでオン/オフできる。
 */
@Mod(MinecraftTweak.MODID)
public class MinecraftTweak {
    public static final String MODID = "minecrafttweak";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecraftTweak() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TweakConfig.SPEC, MODID + ".toml");

        for (Feature feature : Features.all()) {
            feature.register(modBus);
        }

        TextSettings.registerProvider(new QuarkCodes());
        MinecraftForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> TweakCommand.register(e.getDispatcher()));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.eeeeeeruki.minecrafttweak.client.ClientSetup.init();
        }

        LOGGER.info("minEcraft twEEk: {} feature(s) loaded", Features.all().size());
    }
}
