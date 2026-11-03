package com.eeeeeeruki.compacttoasts;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

@Mod(CompactToasts.MODID)
public class CompactToasts {
    public static final String MODID = "compacttoasts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CompactToasts() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientInit.init();
        }
    }
}
