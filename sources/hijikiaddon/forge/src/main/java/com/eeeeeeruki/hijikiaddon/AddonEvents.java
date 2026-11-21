package com.eeeeeeruki.hijikiaddon;

import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HijikiAddon.MOD_ID)
public class AddonEvents {
    @SubscribeEvent
    public static void onDamage(LivingAttackEvent event) {
        if (AddonLogic.onIncomingDamage(event.getEntity(), event.getSource())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        AddonLogic.onLivingTick(event.getEntity());
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (AddonLogic.onLeftClickBlock(event.getEntity(), event.getPos())) event.setCanceled(true);
    }
}
