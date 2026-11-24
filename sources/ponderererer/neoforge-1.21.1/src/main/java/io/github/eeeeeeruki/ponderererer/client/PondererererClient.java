package io.github.eeeeeeruki.ponderererer.client;

import io.github.eeeeeeruki.ponderererer.Ponderererer;
import io.github.eeeeeeruki.ponderererer.demo.DemoPlugin;
import io.github.eeeeeeruki.ponderererer.element.FxPlayer;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;

@Mod(value = Ponderererer.MOD_ID, dist = Dist.CLIENT)
public final class PondererererClient {
    public PondererererClient(IEventBus modBus) {
        PonderIndex.addPlugin(new DemoPlugin());
        NeoForge.EVENT_BUS.addListener(PondererererClient::hideFakePlayerNames);
        NeoForge.EVENT_BUS.addListener(PonderSpriteAnimator::onOpen);
        NeoForge.EVENT_BUS.addListener(PonderSpriteAnimator::onRender);
    }

    private static void hideFakePlayerNames(RenderNameTagEvent event) {
        if (event.getEntity() instanceof FxPlayer) event.setCanRender(TriState.FALSE);
    }
}
