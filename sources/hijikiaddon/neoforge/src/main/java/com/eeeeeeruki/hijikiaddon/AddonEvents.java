package com.eeeeeeruki.hijikiaddon;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = HijikiAddon.MOD_ID)
public class AddonEvents {
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (AddonLogic.onIncomingDamage(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof LivingEntity living) AddonLogic.onLivingTick(living);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (AddonLogic.onLeftClickBlock(event.getEntity(), event.getPos())) event.setCanceled(true);
    }
}
