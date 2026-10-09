package com.example.dronemod;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public class DroneFlightSound extends AbstractTickableSoundInstance {
    private final DroneEntity drone;

    public DroneFlightSound(DroneEntity drone) {
        super(DroneMod.FLIGHT.get(), SoundSource.HOSTILE, SoundInstance.createUnseededRandom());
        this.drone = drone;
        this.looping = true;
        this.delay = 0;
        this.volume = 3.0f;
        this.pitch = 1.0f;
        this.x = drone.getX();
        this.y = drone.getY();
        this.z = drone.getZ();
    }

    @Override
    public void tick() {
        if (drone.isRemoved()) {
            this.stop();
            return;
        }
        this.x = drone.getX();
        this.y = drone.getY();
        this.z = drone.getZ();
    }
}
