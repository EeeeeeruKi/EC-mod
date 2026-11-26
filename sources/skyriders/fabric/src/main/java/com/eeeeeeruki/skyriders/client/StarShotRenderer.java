package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.entity.StarShotEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** 星型弾：光る小さな星（ブロックを組んだだけ）。貫通弾は大きい */
public class StarShotRenderer extends EntityRenderer<StarShotEntity> {
    private final BlockRenderManager blocks;

    public StarShotRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderManager();
        this.shadowRadius = 0.2f;
    }

    @Override
    public void render(StarShotEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        float size = entity.isPierce() ? 0.55f : 0.38f;
        BlockState state = (entity.isPierce() ? Blocks.YELLOW_CONCRETE : Blocks.GOLD_BLOCK).getDefaultState();
        int full = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        matrices.push();
        matrices.translate(0f, 0.3f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, entity.prevSpin, entity.spin)));
        for (int i = 0; i < 5; i++) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 72f));
            matrices.translate(-size * 0.2f, 0f, -size * 0.1f);
            matrices.scale(size * 0.4f, size, size * 0.2f);
            this.blocks.renderBlockAsEntity(state, matrices, vertexConsumers, full, OverlayTexture.DEFAULT_UV);
            matrices.pop();
        }
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(StarShotEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
