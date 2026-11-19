package com.eeeeeeruki.minecrafttweak;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.Features;
import com.eeeeeeruki.minecrafttweak.core.TweakConfig;
import com.eeeeeeruki.minecrafttweak.core.TextSettings;
import com.eeeeeeruki.minecrafttweak.compat.QuarkCodes;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
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

    public MinecraftTweak(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, TweakConfig.SPEC, MODID + ".toml");

        for (Feature feature : Features.all()) {
            feature.register(modBus);
        }

        TextSettings.registerProvider(new QuarkCodes());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> TweakCommand.register(e.getDispatcher()));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.eeeeeeruki.minecrafttweak.client.ClientSetup.init(container, modBus);
        }

        LOGGER.info("minEcraft twEEk: {} feature(s) loaded", Features.all().size());
    }
}
