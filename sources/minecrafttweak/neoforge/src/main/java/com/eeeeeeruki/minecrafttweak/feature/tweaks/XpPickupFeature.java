package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** サンプル機能：経験値オーブを吸い込むときの待ち時間を短くする（バニラは2ティック）。 */
public class XpPickupFeature extends Feature {
    private ModConfigSpec.IntValue delay;

    public XpPickupFeature() {
        super("xp_pickup", FeatureCategory.TWEAKS,
                "Shortens the wait between picking up experience orbs.");
    }

    @Override
    protected void addOptions(ModConfigSpec.Builder builder) {
        delay = intOption(builder, "delay", "Ticks to wait between orbs (vanilla: 2).", 0, 0, 2);
    }

    @Override
    public void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
    }

    private void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!isEnabled() || player.level().isClientSide()) {
            return;
        }
        int max = delay.get();
        if (player.takeXpDelay > max) {
            player.takeXpDelay = max;
        }
    }
}
