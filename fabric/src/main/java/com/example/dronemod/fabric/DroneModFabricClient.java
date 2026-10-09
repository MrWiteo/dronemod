package com.example.dronemod.fabric;

import com.example.dronemod.DroneModel;
import com.example.dronemod.DroneRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class DroneModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityModelLayerRegistry.registerModelLayer(DroneModel.LAYER, DroneModel::createLayer);
        EntityRendererRegistry.register(DroneModFabric.DRONE, DroneRenderer::new);
    }
}
