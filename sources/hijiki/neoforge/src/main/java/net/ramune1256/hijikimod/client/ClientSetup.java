package net.ramune1256.hijikimod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.ramune1256.hijikimod.HijikiMod;
import net.ramune1256.hijikimod.entity.ModEntities;

@EventBusSubscriber(modid = HijikiMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
