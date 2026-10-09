package com.example.dronemod;

import net.minecraft.client.Minecraft;

public class DroneClient {
    public static void startFlightSound(DroneEntity e) {
        Minecraft.getInstance().getSoundManager().play(new DroneFlightSound(e));
    }
}
