package net.ramune1256.hijikimod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.ramune1256.hijikimod.HijikiMod;

/** Blockbench で作ってもらったモデル（Hstome と eyes）をそのまま使う。 */
public class HijiWitherModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(HijikiMod.MOD_ID, "hiji_wither"), "main");
    private final ModelPart hstome;
    private final ModelPart eyes;

    public HijiWitherModel(ModelPart root) {
        this.hstome = root.getChild("Hstome");
        this.eyes = this.hstome.getChild("eyes");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition hstome = partdefinition.addOrReplaceChild("Hstome", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -16.0F, -7.0F, 9.0F, 10.0F, 14.0F, new CubeDeformation(0.0F))
                .texOffs(0, 24).addBox(-1.0F, -17.0F, -8.0F, 3.0F, 3.0F, 5.0F, new CubeDeformation(0.0F))
                .texOffs(0, 32).addBox(-10.0F, -19.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.0F)),
                PartPose.offset(2.0F, 24.0F, 0.0F));

        hstome.addOrReplaceChild("eyes", CubeListBuilder.create()
                .texOffs(16, 24).addBox(3.0F, -13.0F, 3.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(24, 24).addBox(3.0F, -12.0F, -5.0F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // 見た目だけなので、まだ動かさない
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        hstome.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
