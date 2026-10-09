package com.example.dronemod.neoforge;

import com.example.dronemod.DroneEntity;
import com.example.dronemod.DroneMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(DroneMod.MODID)
public class DroneModNeoForge {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DroneMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DroneMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DroneMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE =
            ENTITIES.register("drone", DroneMod::createDroneType);
    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT =
            SOUNDS.register("drone.flight", () -> DroneMod.createSound("drone.flight"));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION =
            SOUNDS.register("drone.explosion", () -> DroneMod.createSound("drone.explosion"));
    public static final DeferredHolder<Item, DeferredSpawnEggItem> DRONE_EGG = ITEMS.register("drone_spawn_egg",
            () -> new DeferredSpawnEggItem(DRONE, 0x777C80, 0x962828, new Item.Properties()));

    public DroneModNeoForge(IEventBus modBus) {
        DroneMod.DRONE = DRONE;
        DroneMod.FLIGHT = FLIGHT;
        DroneMod.EXPLOSION = () -> EXPLOSION;
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(DroneModNeoForge::onAttributes);
        modBus.addListener(DroneModNeoForge::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(DroneModNeoForge::onServerTick);
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(DRONE.get(), DroneMod.droneAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(DRONE_EGG.get());
        }
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        DroneMod.onServerTick(e.getServer());
    }
}
