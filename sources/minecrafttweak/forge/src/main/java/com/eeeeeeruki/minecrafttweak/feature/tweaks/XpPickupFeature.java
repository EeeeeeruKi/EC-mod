package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** サンプル機能：経験値オーブを吸い込むときの待ち時間を短くする（バニラは2ティック）。 */
public class XpPickupFeature extends Feature {
    private ForgeConfigSpec.IntValue delay;

    public XpPickupFeature() {
        super("xp_pickup", FeatureCategory.TWEAKS,
                "Shortens the wait between picking up experience orbs.");
    }

    @Override
    protected void addOptions(ForgeConfigSpec.Builder builder) {
        delay = intOption(builder, "delay", "Ticks to wait between orbs (vanilla: 2).", 0, 0, 2);
    }

    @Override
    public void register(IEventBus modBus) {
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (!isEnabled() || player.level().isClientSide()) {
            return;
        }
        int max = delay.get();
        if (player.takeXpDelay > max) {
            player.takeXpDelay = max;
        }
    }
}
