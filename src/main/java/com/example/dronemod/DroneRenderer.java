package com.example.dronemod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class DroneRenderer extends MobRenderer<DroneEntity, DroneModel> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(DroneMod.MODID, "textures/entity/drone.png");

    public DroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new DroneModel(ctx.bakeLayer(DroneModel.LAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity e) {
        return TEXTURE;
    }

    @Override
    protected void setupRotations(DroneEntity e, PoseStack ps, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(e, ps, bob, yBodyRot, partialTick, scale);
        ps.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, e.xRotO, e.getXRot())));
    }
}
