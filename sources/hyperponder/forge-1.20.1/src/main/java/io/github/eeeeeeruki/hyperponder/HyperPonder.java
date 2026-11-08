package io.github.eeeeeeruki.hyperponder;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod("hyperponder")
public class HyperPonder {
    public HyperPonder() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClientInit::register);
    }
}
