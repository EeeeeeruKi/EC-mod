package com.eeeeeeruki.skyriders.client;

import com.eeeeeeruki.skyriders.entity.MachineEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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

/** 仮マシンの見た目：ブロックを組んだだけの星っぽい板。あとでモデルに差し替える。 */
public class MachineRenderer extends EntityRenderer<MachineEntity> {
    private final BlockRenderManager blocks;

    public MachineRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.blocks = ctx.getBlockRenderManager();
        this.shadowRadius = 0.7f;
    }

    @Override
    public void render(MachineEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        // 上昇の限界：点滅（2tickごとに消える）
        if (entity.isBlinking() && (entity.age / 2) % 2 == 0) {
            super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
            return;
        }
        matrices.push();

        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevYaw, entity.getYaw());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-bodyYaw)); // モデルの+Zが前

        int spin = entity.getSpinTicks();
        if (spin > 0) {
            float p = (MachineEntity.spinDuration() - spin + tickDelta) / MachineEntity.spinDuration();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(p * 360f));
        }
        // 前後の傾き（+で機首が下がる）。真ん中あたりを軸にする
        float tilt = MathHelper.lerp(tickDelta, entity.prevVisPitch, entity.visPitch);
        matrices.translate(0f, 0.18f, 0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
        float roll = MathHelper.lerp(tickDelta, entity.prevRoll, entity.roll);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(roll));
        matrices.translate(0f, -0.18f, 0f);

        BlockState body = Blocks.GOLD_BLOCK.getDefaultState();
        BlockState tip = Blocks.YELLOW_CONCRETE.getDefaultState();
        BlockState core = Blocks.ORANGE_CONCRETE.getDefaultState();

        // 本体（ひし形）
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45f));
        cube(matrices, vertexConsumers, light, body, -0.45f, 0.05f, -0.45f, 0.9f, 0.22f, 0.9f);
        matrices.pop();
        // 星の5つの先っぽ
        for (int i = 0; i < 5; i++) {
            matrices.push();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 72f));
            cube(matrices, vertexConsumers, light, tip, -0.16f, 0.08f, 0.45f, 0.32f, 0.16f, 0.45f);
            matrices.pop();
        }
        // 真ん中
        cube(matrices, vertexConsumers, light, core, -0.2f, 0.26f, -0.2f, 0.4f, 0.06f, 0.4f);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void cube(MatrixStack m, VertexConsumerProvider v, int light, BlockState state,
                      float x, float y, float z, float sx, float sy, float sz) {
        m.push();
        m.translate(x, y, z);
        m.scale(sx, sy, sz);
        this.blocks.renderBlockAsEntity(state, m, v, light, OverlayTexture.DEFAULT_UV);
        m.pop();
    }

    @Override
    public Identifier getTexture(MachineEntity entity) {
        return SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE;
    }
}
