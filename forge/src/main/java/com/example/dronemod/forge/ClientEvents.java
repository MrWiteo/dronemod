package com.example.dronemod.forge;

import com.example.dronemod.DroneMod;
import com.example.dronemod.DroneModel;
import com.example.dronemod.DroneRenderer;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DroneMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DroneModel.LAYER, DroneModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DroneModForge.DRONE.get(), DroneRenderer::new);
    }
}
