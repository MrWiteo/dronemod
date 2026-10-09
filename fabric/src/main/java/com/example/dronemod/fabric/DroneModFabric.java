package com.example.dronemod.fabric;

import com.example.dronemod.DroneEntity;
import com.example.dronemod.DroneMod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

public class DroneModFabric implements ModInitializer {
    public static EntityType<DroneEntity> DRONE;
    public static Item DRONE_EGG;

    @Override
    public void onInitialize() {
        DRONE = Registry.register(BuiltInRegistries.ENTITY_TYPE, DroneMod.id("drone"), DroneMod.createDroneType());
        SoundEvent flight = Registry.register(BuiltInRegistries.SOUND_EVENT, DroneMod.id("drone.flight"),
                DroneMod.createSound("drone.flight"));
        Holder<SoundEvent> explosion = Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT,
                DroneMod.id("drone.explosion"), DroneMod.createSound("drone.explosion"));
        DroneMod.DRONE = () -> DRONE;
        DroneMod.FLIGHT = () -> flight;
        DroneMod.EXPLOSION = () -> explosion;

        DRONE_EGG = Registry.register(BuiltInRegistries.ITEM, DroneMod.id("drone_spawn_egg"),
                new SpawnEggItem(DRONE, 0x777C80, 0x962828, new Item.Properties()));

        FabricDefaultAttributeRegistry.register(DRONE, DroneMod.droneAttributes());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.SPAWN_EGGS).register(entries -> entries.accept(DRONE_EGG));
        ServerTickEvents.END_SERVER_TICK.register(DroneMod::onServerTick);
    }
}
