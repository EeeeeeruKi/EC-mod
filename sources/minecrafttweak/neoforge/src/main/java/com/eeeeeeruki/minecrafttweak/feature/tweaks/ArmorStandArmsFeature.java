package com.eeeeeeruki.minecrafttweak.feature.tweaks;

import com.eeeeeeruki.minecrafttweak.core.Feature;
import com.eeeeeeruki.minecrafttweak.core.FeatureCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** サンプル機能：新しく置いた防具立てに最初から腕を付ける。 */
public class ArmorStandArmsFeature extends Feature {
    public ArmorStandArmsFeature() {
        super("armor_stand_arms", FeatureCategory.TWEAKS, true,
                "Newly placed armor stands have arms.");
    }

    @Override
    public void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(this::onEntityJoin);
    }

    private void onEntityJoin(EntityJoinLevelEvent event) {
        if (!isEnabled() || event.getLevel().isClientSide() || event.loadedFromDisk()) {
            return;
        }
        if (event.getEntity() instanceof ArmorStand stand) {
            byte flags = stand.getEntityData().get(ArmorStand.DATA_CLIENT_FLAGS);
            stand.getEntityData().set(ArmorStand.DATA_CLIENT_FLAGS, (byte) (flags | ArmorStand.CLIENT_FLAG_SHOW_ARMS));
        }
    }
}
