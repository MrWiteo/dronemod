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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Loader-independent mod core. Each loader module registers things and fills the suppliers below. */
public final class DroneMod {
    public static final String MODID = "dronemod";

    // Average delay between drone raids: MIN..MIN+RANGE ticks (20 ticks = 1 sec)
    private static final int RAID_MIN_TICKS = 6000;   // 5 min
    private static final int RAID_RANGE_TICKS = 18000; // + up to 15 min

    // Filled in by the loader entry points at registration time.
    public static Supplier<EntityType<DroneEntity>> DRONE;
    public static Supplier<SoundEvent> FLIGHT;
    public static Supplier<Holder<SoundEvent>> EXPLOSION;

    private static int raidTimer = 3000 + ThreadLocalRandom.current().nextInt(RAID_RANGE_TICKS);

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
        raidTimer = RAID_MIN_TICKS + ThreadLocalRandom.current().nextInt(RAID_RANGE_TICKS);

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
