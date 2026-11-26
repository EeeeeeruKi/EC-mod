package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.SkyRiders;
import com.eeeeeeruki.skyriders.entity.MachineEntity;
import com.eeeeeeruki.skyriders.item.PatchItem;
import com.eeeeeeruki.skyriders.net.MachineActionPayload;
import com.eeeeeeruki.skyriders.net.MachineHitPayload;
import com.eeeeeeruki.skyriders.net.CourseScreenPayload;
import com.eeeeeeruki.skyriders.net.RaceStatePayload;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.item.Item;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.Perspective;
import net.minecraft.text.Text;

public class SkyRidersClient implements ClientModInitializer {
    private static boolean riding;
    private static Perspective savedPerspective;

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(SkyRiders.MACHINE, MachineRenderer::new);
        EntityRendererRegistry.register(SkyRiders.STAR_SHOT, StarShotRenderer::new);

        MachineEntity.clientActionSender = action -> {
            if (ClientPlayNetworking.canSend(MachineActionPayload.ID)) {
                ClientPlayNetworking.send(new MachineActionPayload(action));
            }
        };

        // 攻撃を受けた時の吹っ飛び
        ClientPlayNetworking.registerGlobalReceiver(MachineHitPayload.ID, (payload, context) -> context.client().execute(() -> {
            if (context.client().world != null
                    && context.client().world.getEntityById(payload.entityId()) instanceof MachineEntity m) {
                m.applyHitClient(payload.vx(), payload.vy(), payload.vz(), payload.stun());
            }
        }));

        // コースの管理画面とレースの表示
        ClientPlayNetworking.registerGlobalReceiver(CourseScreenPayload.ID,
                (payload, context) -> context.client().execute(() -> CourseScreen.receive(payload.json())));
        ClientPlayNetworking.registerGlobalReceiver(RaceStatePayload.ID,
                (payload, context) -> context.client().execute(() -> RaceHud.receive(payload)));
        HudRenderCallback.EVENT.register(RaceHud::render);
        ClientTickEvents.END_CLIENT_TICK.register(c -> RaceHud.tick());
        BlockRenderLayerMap.INSTANCE.putBlocks(RenderLayer.getTranslucent(), SkyRiders.GOAL_BLOCK, SkyRiders.CHECKPOINT_BLOCK);

        // パッチの色
        List<Item> patches = new ArrayList<>(SkyRiders.PATCHES.values());
        patches.add(SkyRiders.PATCH_ALL);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof PatchItem p
                ? 0xFF000000 | p.color() : 0xFFFFFFFF, patches.toArray(new Item[0]));

        // ワールドより先に入力を読む（バニラのQ・Eより先に横取りする）
        ClientTickEvents.START_CLIENT_TICK.register(SkyRidersClient::readInput);
        HudRenderCallback.EVENT.register(MachineHud::render);
    }

    private static void readInput(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        MachineEntity machine = player != null && player.getVehicle() instanceof MachineEntity m
                && m.getControllingPassenger() == player ? m : null;

        // 乗った・降りた時のカメラ切り替え
        if (machine != null && !riding) {
            riding = true;
            savedPerspective = client.options.getPerspective();
            client.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            client.inGameHud.setOverlayMessage(Text.translatable("message.skyriders.mounted"), false);
        } else if (machine == null && riding) {
            riding = false;
            if (savedPerspective != null) client.options.setPerspective(savedPerspective);
        }
        if (machine == null) return;

        GameOptions o = client.options;
        machine.inForward = o.forwardKey.isPressed();
        machine.inBack = o.backKey.isPressed();
        machine.inLeft = o.leftKey.isPressed();
        machine.inRight = o.rightKey.isPressed();
        machine.inCharge = o.jumpKey.isPressed() || o.sneakKey.isPressed();

        // Q：スペシャル（アイテムを捨てない）
        boolean q = false;
        while (o.dropKey.wasPressed()) q = true;
        if (q) MachineEntity.clientActionSender.accept(MachineActionPayload.SPECIAL);

        // E：降りる／乗り換え（インベントリを開かない）
        boolean e = false;
        while (o.inventoryKey.wasPressed()) e = true;
        if (e) MachineEntity.clientActionSender.accept(MachineActionPayload.EXIT_OR_SWAP);
    }
}
