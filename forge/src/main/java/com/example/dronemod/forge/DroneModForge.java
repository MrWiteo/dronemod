package com.example.dronemod.forge;

import com.example.dronemod.DroneEntity;
import com.example.dronemod.DroneMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

@Mod(DroneMod.MODID)
public class DroneModForge {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DroneMod.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, DroneMod.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, DroneMod.MODID);

    public static final RegistryObject<EntityType<DroneEntity>> DRONE =
            ENTITIES.register("drone", DroneMod::createDroneType);
    public static final RegistryObject<SoundEvent> FLIGHT =
            SOUNDS.register("drone.flight", () -> DroneMod.createSound("drone.flight"));
    public static final RegistryObject<SoundEvent> EXPLOSION =
            SOUNDS.register("drone.explosion", () -> DroneMod.createSound("drone.explosion"));
    public static final RegistryObject<Item> DRONE_EGG = ITEMS.register("drone_spawn_egg",
            () -> new ForgeSpawnEggItem(DRONE, 0x777C80, 0x962828, new Item.Properties()));

    public DroneModForge() {
        DroneMod.DRONE = DRONE;
        DroneMod.FLIGHT = FLIGHT;
        DroneMod.EXPLOSION = () -> EXPLOSION.getHolder().orElseThrow();
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(DroneModForge::onAttributes);
        modBus.addListener(DroneModForge::onCreativeTab);
        MinecraftForge.EVENT_BUS.addListener(DroneModForge::onServerTick);
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(DRONE.get(), DroneMod.droneAttributes().build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(DRONE_EGG.get());
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase == TickEvent.Phase.END) {
            DroneMod.onServerTick(e.getServer());
        }
    }
}
