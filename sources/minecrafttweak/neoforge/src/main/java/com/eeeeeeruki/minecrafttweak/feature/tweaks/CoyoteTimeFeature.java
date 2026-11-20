package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * コヨーテタイム。足場から歩いて落ちたあと、少しのあいだならまだジャンプできる。
 * ジャンプは自分の画面（クライアント）で決まるので、クライアントだけで動く。
 */
public class CoyoteTimeFeature extends Feature {
    public static CoyoteTimeFeature INSTANCE;
    public ModConfigSpec.IntValue ticks;

    public CoyoteTimeFeature() {
        super("coyote_time", FeatureCategory.TWEAKS,
                "Coyote time: you can still jump for a moment after walking off a ledge.");
        INSTANCE = this;
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        ticks = intOption(builder, "ticks", "How long you can still jump after leaving the ground (ticks, 20 = 1 second).", 5, 1, 20);
    }

    @Override
    public void registerClient(IEventBus modBus) {
        com.eeeeeeruki.minecrafttweak.client.feature.CoyoteTime.register();
    }
}
