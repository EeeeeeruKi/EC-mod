package io.github.eeeeeeruki.ponderererer;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FxConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DEMO_SCENES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        DEMO_SCENES = b.comment("Show the Ponderererer demo scenes on the Lightning Rod (needs a restart).")
                .define("demoScenes", false);
        SPEC = b.build();
    }

    private FxConfig() {}
}
