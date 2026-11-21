package net.ramune1256.hijikimod.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.ramune1256.hijikimod.HijikiMod;
import net.ramune1256.hijikimod.entity.ModEntities;

@Mod.EventBusSubscriber(modid = HijikiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(HijiWitherModel.LAYER_LOCATION, HijiWitherModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.HIJI_WITHER.get(), HijiWitherRenderer::new);
    }
}
