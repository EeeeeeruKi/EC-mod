package net.ramune1256.hijikimod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.ramune1256.hijikimod.HijikiMod;
import net.ramune1256.hijikimod.entity.HijiWitherEntity;

public class HijiWitherRenderer extends MobRenderer<HijiWitherEntity, HijiWitherModel<HijiWitherEntity>> {
    /** モデル（1ブロックの立方体）を何倍で描くか。ウィザーよりひと回り小さく。 */
    private static final float SCALE = 2.5F;
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(HijikiMod.MOD_ID, "textures/entity/hiji_wither.png");

    public HijiWitherRenderer(EntityRendererProvider.Context context) {
        super(context, new HijiWitherModel<>(context.bakeLayer(HijiWitherModel.LAYER_LOCATION)), 1.2F);
    }

    @Override
    public ResourceLocation getTextureLocation(HijiWitherEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(HijiWitherEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(SCALE, SCALE, SCALE);
    }
}
