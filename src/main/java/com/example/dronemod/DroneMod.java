package com.example.dronemod;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
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
public class DroneMod {
    public static final String MODID = "dronemod";

    // Average delay between drone raids: MIN..MIN+RANGE ticks (20 ticks = 1 sec)
    private static final int RAID_MIN_TICKS = 6000;   // 5 min
    private static final int RAID_RANGE_TICKS = 18000; // + up to 15 min

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITIES.register("drone",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(1.6f, 0.6f).clientTrackingRange(10).updateInterval(2).fireImmune().build("drone"));

    public static final DeferredHolder<SoundEvent, SoundEvent> FLIGHT = SOUNDS.register("drone.flight",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "drone.flight")));
    public static final DeferredHolder<SoundEvent, SoundEvent> EXPLOSION = SOUNDS.register("drone.explosion",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MODID, "drone.explosion")));

    public static final DeferredHolder<Item, DeferredSpawnEggItem> DRONE_EGG = ITEMS.register("drone_spawn_egg",
            () -> new DeferredSpawnEggItem(DRONE, 0x777C80, 0x962828, new Item.Properties()));

    private static int raidTimer = 3000 + ThreadLocalRandom.current().nextInt(RAID_RANGE_TICKS);

    public DroneMod(IEventBus modBus) {
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(DroneMod::onAttributes);
        modBus.addListener(DroneMod::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(DroneMod::onServerTick);
    }

    private static void onAttributes(EntityAttributeCreationEvent e) {
        e.put(DRONE.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).build());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(DRONE_EGG.get());
        }
    }

    private static void onServerTick(ServerTickEvent.Post e) {
        if (--raidTimer > 0) return;
        raidTimer = RAID_MIN_TICKS + ThreadLocalRandom.current().nextInt(RAID_RANGE_TICKS);

        MinecraftServer server = e.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.level().dimension() == Level.OVERWORLD && !p.isSpectator()).toList();
        if (players.isEmpty()) return;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        ServerPlayer pl = players.get(rnd.nextInt(players.size()));
        ServerLevel level = pl.serverLevel();
        double ang = rnd.nextDouble() * Math.PI * 2;
        double x = pl.getX() + Math.cos(ang) * 64;
        double z = pl.getZ() + Math.sin(ang) * 64;
        double y = Math.min(pl.getY() + 45, level.getMaxBuildHeight() - 5);
        if (!level.hasChunkAt(BlockPos.containing(x, y, z))) return;

        DroneEntity d = DRONE.get().create(level);
        if (d == null) return;
        d.moveTo(x, y, z, 0f, 0f);
        level.addFreshEntity(d);
    }
}
