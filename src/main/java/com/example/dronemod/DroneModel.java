package com.example.dronemod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public class DroneModel extends EntityModel<DroneEntity> {
    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(DroneMod.MODID, "drone"), "main");
    private final ModelPart root;

    public DroneModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition p = mesh.getRoot();
        p.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2f, -2f, -12f, 4f, 4f, 24f), PartPose.offset(0f, 20f, 0f));
        p.addOrReplaceChild("wings", CubeListBuilder.create().texOffs(0, 28).addBox(-14f, -0.5f, -2f, 28f, 1f, 8f), PartPose.offset(0f, 20f, -3f));
        p.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 40).addBox(-5f, -0.5f, 0f, 10f, 1f, 4f), PartPose.offset(0f, 20f, 8f));
        p.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(0, 46).addBox(-0.5f, -5f, 0f, 1f, 5f, 4f), PartPose.offset(0f, 19f, 8f));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(DroneEntity e, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack ps, VertexConsumer vc, int light, int overlay, int color) {
        root.render(ps, vc, light, overlay, color);
    }
}
