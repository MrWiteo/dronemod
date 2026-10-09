package com.example.dronemod;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Loader-independent mod core. Each loader module registers things and fills the suppliers below. */
public final class DroneMod {
    public static final String MODID = "dronemod";

    // A drone flies in every RAID_INTERVAL_TICKS (20 ticks = 1 sec) for each player in the Overworld.
    private static final int RAID_INTERVAL_TICKS = 6000;  // 5 min
    // Chance that a drone attacks the player instead of just flying past towards the nearest village.
    private static final double ATTACK_CHANCE = 0.10;
    private static final int VILLAGE_SEARCH_CHUNKS = 48;

    // Filled in by the loader entry points at registration time.
    public static Supplier<EntityType<DroneEntity>> DRONE;
    public static Supplier<SoundEvent> FLIGHT;
    public static Supplier<Holder<SoundEvent>> EXPLOSION;

    private static int raidTimer = RAID_INTERVAL_TICKS;

    private DroneMod() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    public static EntityType<DroneEntity> createDroneType() {
        return EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                .sized(1.6f, 0.6f).clientTrackingRange(10).updateInterval(2).fireImmune().build("drone");
    }

    public static SoundEvent createSound(String path) {
        return SoundEvent.createVariableRangeEvent(id(path));
    }

    public static AttributeSupplier.Builder droneAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0);
    }

    /** Call once per server tick (end of tick). */
    public static void onServerTick(MinecraftServer server) {
        if (--raidTimer > 0) return;
        raidTimer = RAID_INTERVAL_TICKS;

        List<ServerPlayer> players = server.getPlayerList().getPlayers().stream()
                .filter(p -> p.level().dimension() == Level.OVERWORLD && !p.isSpectator()).toList();
        for (ServerPlayer pl : players) {
            spawnRaid(pl);
        }
    }

    /** A drone appears to the west of the player. 10%: it circles over the player and attacks, otherwise it flies on towards the nearest village. */
    private static void spawnRaid(ServerPlayer pl) {
        ServerLevel level = pl.serverLevel();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double x = pl.getX() - 64;
        double z = pl.getZ() + (rnd.nextDouble() - 0.5) * 40;
        double y = Math.min(pl.getY() + 45, level.getMaxBuildHeight() - 5);
        if (!level.hasChunkAt(BlockPos.containing(x, y, z))) return;

        DroneEntity d = DRONE.get().create(level);
        if (d == null) return;
        d.moveTo(x, y, z, 0f, 0f);

        if (rnd.nextDouble() < ATTACK_CHANCE) {
            d.startHunt();
        } else {
            BlockPos village = level.findNearestMapStructure(StructureTags.VILLAGE, pl.blockPosition(), VILLAGE_SEARCH_CHUNKS, false);
            if (village != null) {
                d.startFlyby(village.getX() + 0.5, village.getZ() + 0.5);
            } else {
                d.startFlyby(x + 1000, z); // no village around: just fly east
            }
        }
        level.addFreshEntity(d);
    }
}
