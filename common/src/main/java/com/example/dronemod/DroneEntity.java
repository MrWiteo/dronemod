package com.example.dronemod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;

public class DroneEntity extends Mob {
    private static final double CRUISE_SPEED = 0.9;    // blocks per tick
    private static final double DIVE_SPEED = 2.2;
    private static final double ALTITUDE = 45;
    private static final double ORBIT_RADIUS = 45;
    private static final float EXPLOSION_POWER = 8.0f; // TNT = 4

    // How long the drone circles before it tries to dive: MIN..MIN+RANGE ticks (20 ticks = 1 sec)
    private static final int CIRCLE_MIN_TICKS = 600;    // 30 sec
    private static final int CIRCLE_RANGE_TICKS = 1200; // + up to 60 sec
    private static final int RETRY_TICKS = 100;         // look for a target again every 5 sec
    private static final int HUNT_GIVE_UP_TICKS = 3600; // flies away after 3 min without a target
    private static final int FLYBY_LIFETIME = 4800;     // fly-by drones disappear after 4 min

    // Build detection (scores are in half-points: a normal block = 2, a log = 1, a furnace/chest/etc. = 10)
    private static final int SCAN_RADIUS = 128;         // blocks around the player (loaded chunks only)
    private static final int SCAN_STEP = 4;
    private static final int MAX_DETAILED = 400;        // candidate columns that get a detailed check
    private static final int MIN_BUILD_SCORE = 40;      // ~20 normal blocks

    private enum Mode { HUNT, FLYBY }

    private Mode mode = Mode.HUNT;
    private double flyDirX = 1, flyDirZ = 0;
    private boolean diving, exploded, soundStarted;
    private int diveAt, diveTicks, huntStart;
    private Vec3 target;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        startHunt();
    }

    /** Circle over the player, then dive onto the nearest build (or a random house if the player is in a village). */
    public void startHunt() {
        this.mode = Mode.HUNT;
        this.huntStart = this.tickCount;
        this.diveAt = this.tickCount + CIRCLE_MIN_TICKS + this.random.nextInt(CIRCLE_RANGE_TICKS);
    }

    /** Fly in a straight line towards the given point (a village) and carry on past it. */
    public void startFlyby(double toX, double toZ) {
        this.mode = Mode.FLYBY;
        double dx = toX - getX(), dz = toZ - getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1) {
            dx = 1;
            dz = 0;
            len = 1;
        }
        this.flyDirX = dx / len;
        this.flyDirZ = dz / len;
        face(new Vec3(flyDirX, 0, flyDirZ));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flyby", mode == Mode.FLYBY);
        tag.putDouble("FlyDirX", flyDirX);
        tag.putDouble("FlyDirZ", flyDirZ);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.getBoolean("Flyby")) {
            this.mode = Mode.FLYBY;
            this.flyDirX = tag.getDouble("FlyDirX");
            this.flyDirZ = tag.getDouble("FlyDirZ");
        }
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void travel(Vec3 v) {
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (!soundStarted) {
                soundStarted = true;
                DroneClient.startFlightSound(this);
            }
            return;
        }
        ServerLevel sl = (ServerLevel) this.level();
        if (exploded) return;

        if (diving) {
            dive(sl);
            return;
        }
        if (mode == Mode.FLYBY) {
            flybyTick(sl);
        } else {
            huntTick(sl);
        }
    }

    private void flybyTick(ServerLevel sl) {
        Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 400, EntitySelector.NO_SPECTATORS);
        if (p == null || tickCount > FLYBY_LIFETIME) {
            this.discard();
            return;
        }
        flyAlong(sl, p, flyDirX, flyDirZ);
    }

    private void huntTick(ServerLevel sl) {
        Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 512, EntitySelector.NO_SPECTATORS);
        if (p == null || tickCount - huntStart > HUNT_GIVE_UP_TICKS) {
            this.discard();
            return;
        }
        orbit(sl, p);
        if (tickCount >= diveAt && tickCount % 20 == 0) {
            Vec3 t = findTarget(sl, p);
            if (t != null) {
                target = t;
                diving = true;
                diveTicks = 0;
            } else {
                diveAt = tickCount + RETRY_TICKS;
            }
        }
    }

    private double wantedAltitude(ServerLevel sl, Player p) {
        double wantY = p.getY() + ALTITUDE;
        if (sl.hasChunkAt(this.blockPosition())) {
            int h = sl.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(getX()), Mth.floor(getZ()));
            wantY = Math.max(wantY, h + 20);
        }
        return Math.min(wantY, sl.getMaxBuildHeight() - 3);
    }

    private void steer(double dirX, double dirZ, double wantY) {
        double dy = Mth.clamp((wantY - getY()) * 0.05, -0.3, 0.3);
        if (horizontalCollision || verticalCollision) dy = 0.6;
        Vec3 want = new Vec3(dirX * CRUISE_SPEED, dy, dirZ * CRUISE_SPEED);
        this.setDeltaMovement(this.getDeltaMovement().lerp(want, 0.1));
        face(this.getDeltaMovement());
    }

    private void flyAlong(ServerLevel sl, Player p, double dirX, double dirZ) {
        steer(dirX, dirZ, wantedAltitude(sl, p));
    }

    private void orbit(ServerLevel sl, Player p) {
        double dx = p.getX() - getX(), dz = p.getZ() - getZ();
        double dist = Math.max(Math.sqrt(dx * dx + dz * dz), 0.001);
        double nx = dx / dist, nz = dz / dist;
        double dirX, dirZ;
        if (dist > ORBIT_RADIUS + 10) {
            dirX = nx;
            dirZ = nz;
        } else {
            double k = (dist - ORBIT_RADIUS) / 20.0;
            dirX = -nz + nx * k;
            dirZ = nx + nz * k;
            double l = Math.max(Math.sqrt(dirX * dirX + dirZ * dirZ), 0.001);
            dirX /= l;
            dirZ /= l;
        }
        steer(dirX, dirZ, wantedAltitude(sl, p));
    }

    private void dive(ServerLevel sl) {
        diveTicks++;
        Vec3 to = target.subtract(this.position());
        double d = to.length();
        boolean hit = diveTicks > 3 && (horizontalCollision || verticalCollision || onGround());
        if (hit || d < 3 || diveTicks > 900) {
            explode(sl);
            return;
        }
        Vec3 vel = to.scale(DIVE_SPEED / d);
        this.setDeltaMovement(vel);
        face(vel);
    }

    private void face(Vec3 v) {
        if (v.lengthSqr() < 1.0E-6) return;
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        float yaw = (float) (Mth.atan2(-v.x, v.z) * (180.0 / Math.PI));
        float pitch = (float) (-(Mth.atan2(v.y, h) * (180.0 / Math.PI)));
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void explode(ServerLevel sl) {
        exploded = true;
        double x = getX(), y = getY(), z = getZ();
        sl.explode(this, null, null, x, y, z, EXPLOSION_POWER, true, Level.ExplosionInteraction.MOB,
                ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, DroneMod.EXPLOSION.get());
        // extra fires around the impact
        for (int i = 0; i < 160; i++) {
            BlockPos p = BlockPos.containing(x + random.nextInt(25) - 12, y + random.nextInt(17) - 8, z + random.nextInt(25) - 12);
            if (sl.getBlockState(p).isAir() && BaseFireBlock.canBePlacedAt(sl, p, Direction.UP)) {
                sl.setBlockAndUpdate(p, BaseFireBlock.getState(sl, p));
            }
        }
        this.discard();
    }

    // ------------------------------------------------------------------ target search

    private Vec3 findTarget(ServerLevel sl, Player pl) {
        Vec3 house = findVillageHouse(sl, pl);
        if (house != null) return house;
        return findNearestBuild(sl, pl);
    }

    /** If the player stands inside a village, returns a random house of that village. */
    private Vec3 findVillageHouse(ServerLevel sl, Player pl) {
        StructureStart start = sl.structureManager().getStructureWithPieceAt(pl.blockPosition(), StructureTags.VILLAGE);
        if (!start.isValid()) return null;

        List<BoundingBox> houses = new ArrayList<>();
        List<BoundingBox> all = new ArrayList<>();
        for (StructurePiece piece : start.getPieces()) {
            BoundingBox bb = piece.getBoundingBox();
            all.add(bb);
            if (bb.getYSpan() >= 5 && bb.getXSpan() <= 24 && bb.getZSpan() <= 24) houses.add(bb);
        }
        List<BoundingBox> pool = houses.isEmpty() ? all : houses;
        if (pool.isEmpty()) return null;

        BoundingBox bb = pool.get(this.random.nextInt(pool.size()));
        int x = bb.getCenter().getX(), z = bb.getCenter().getZ();
        if (!loaded(sl, x, z)) return null;
        int top = sl.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        return new Vec3(x + 0.5, top - 1, z + 0.5);
    }

    /** Nearest man-made build around the player, even if it is far away (within loaded chunks). */
    private Vec3 findNearestBuild(ServerLevel sl, Player pl) {
        int px = pl.getBlockX(), pz = pl.getBlockZ();
        List<int[]> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int ox = -SCAN_RADIUS; ox <= SCAN_RADIUS; ox += SCAN_STEP) {
            for (int oz = -SCAN_RADIUS; oz <= SCAN_RADIUS; oz += SCAN_STEP) {
                int cx = px + ox, cz = pz + oz;
                if (!loaded(sl, cx, cz)) continue;
                int top = sl.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                m.set(cx, top - 1, cz);
                if (isNatural(sl.getBlockState(m))) continue; // cheap pre-check: the surface block must be man-made
                candidates.add(new int[] { cx, cz, top, ox * ox + oz * oz });
            }
        }
        candidates.sort(Comparator.comparingInt(c -> c[3]));
        int n = Math.min(candidates.size(), MAX_DETAILED);
        for (int i = 0; i < n; i++) {
            int[] c = candidates.get(i);
            if (buildScore(sl, c[0], c[1], c[2]) >= MIN_BUILD_SCORE) {
                return new Vec3(c[0] + 0.5, c[2] - 1, c[1] + 0.5);
            }
        }
        return null;
    }

    private static int buildScore(ServerLevel sl, int cx, int cz, int top) {
        int score = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int yy = top - 8; yy < top; yy++) {
                    m.set(cx + dx, yy, cz + dz);
                    BlockState s = sl.getBlockState(m);
                    if (s.isAir()) continue;
                    if (s.hasBlockEntity() || s.is(Blocks.CRAFTING_TABLE) || s.is(BlockTags.BEDS)) {
                        score += 10;               // furnaces, chests, crafting tables, beds...
                    } else if (s.is(BlockTags.LOGS)) {
                        score += 1;                // could be a tree, so it counts for less
                    } else if (!isNatural(s)) {
                        score += 2;
                    }
                }
            }
        }
        return score;
    }

    private static boolean loaded(ServerLevel sl, int x, int z) {
        return sl.hasChunkAt(new BlockPos(x, 64, z));
    }

    /** Blocks that are not counted as a "build" (logs are handled separately in buildScore). */
    private static boolean isNatural(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty()
                || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(BlockTags.LEAVES) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.TERRACOTTA)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY);
    }
}
