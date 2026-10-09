package com.example.dronemod.neoforge;

import com.example.dronemod.DroneMod;
import com.example.dronemod.DroneModel;
import com.example.dronemod.DroneRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = DroneMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(DroneModel.LAYER, DroneModel::createLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(DroneModNeoForge.DRONE.get(), DroneRenderer::new);
    }
}
