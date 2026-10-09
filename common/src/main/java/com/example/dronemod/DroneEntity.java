package com.example.dronemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public class DroneEntity extends Mob {
    private static final double CRUISE_SPEED = 0.9;   // blocks per tick
    private static final double DIVE_SPEED = 2.2;
    private static final double ALTITUDE = 45;
    private static final double ORBIT_RADIUS = 45;
    private static final float EXPLOSION_POWER = 8.0f; // TNT = 4
    private static final int MIN_BUILD_SCORE = 120;     // non-natural blocks in a 7x7x9 area
    private static final int MAX_LIFETIME = 24000;      // flies away after 20 min if no build found

    private boolean diving, exploded, soundStarted;
    private int diveAt, diveTicks;
    private Vec3 target;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setPersistenceRequired();
        this.diveAt = 1200 + this.random.nextInt(4800); // dive in 1..5 min after spawn
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
        Player p = sl.getNearestPlayer(getX(), getY(), getZ(), 512, EntitySelector.NO_SPECTATORS);
        if (p == null || tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }
        cruise(sl, p);
        if (tickCount >= diveAt && tickCount % 20 == 0) {
            Vec3 t = findTarget(sl, p);
            if (t != null) {
                target = t;
                diving = true;
                diveTicks = 0;
            } else {
                diveAt = tickCount + 200; // retry in 10 sec
            }
        }
    }

    private void cruise(ServerLevel sl, Player p) {
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
        double wantY = p.getY() + ALTITUDE;
        if (sl.hasChunkAt(this.blockPosition())) {
            int h = sl.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(getX()), Mth.floor(getZ()));
            wantY = Math.max(wantY, h + 20);
        }
        wantY = Math.min(wantY, sl.getMaxBuildHeight() - 3);
        double dy = Mth.clamp((wantY - getY()) * 0.05, -0.3, 0.3);
        if (horizontalCollision || verticalCollision) dy = 0.6;
        Vec3 want = new Vec3(dirX * CRUISE_SPEED, dy, dirZ * CRUISE_SPEED);
        this.setDeltaMovement(this.getDeltaMovement().lerp(want, 0.1));
        face(this.getDeltaMovement());
    }

    private void dive(ServerLevel sl) {
        diveTicks++;
        Vec3 to = target.subtract(this.position());
        double d = to.length();
        boolean hit = diveTicks > 3 && (horizontalCollision || verticalCollision || onGround());
        if (hit || d < 3 || diveTicks > 600) {
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

    /** Looks for the densest cluster of non-natural blocks around the player. */
    private Vec3 findTarget(ServerLevel sl, Player pl) {
        int best = 0;
        Vec3 bestPos = null;
        for (int ox = -48; ox <= 48; ox += 8) {
            for (int oz = -48; oz <= 48; oz += 8) {
                int cx = pl.getBlockX() + ox, cz = pl.getBlockZ() + oz;
                if (!loaded(sl, cx - 4, cz - 4) || !loaded(sl, cx + 4, cz - 4)
                        || !loaded(sl, cx - 4, cz + 4) || !loaded(sl, cx + 4, cz + 4)) continue;
                int top = sl.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
                int score = 0;
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        for (int yy = top - 9; yy < top; yy++) {
                            m.set(cx + dx, yy, cz + dz);
                            if (!isNatural(sl.getBlockState(m))) score++;
                        }
                    }
                }
                if (score > best) {
                    best = score;
                    bestPos = new Vec3(cx + 0.5, top - 1, cz + 0.5);
                }
            }
        }
        return best >= MIN_BUILD_SCORE ? bestPos : null;
    }

    private static boolean loaded(ServerLevel sl, int x, int z) {
        return sl.hasChunkAt(new BlockPos(x, 64, z));
    }

    private static boolean isNatural(BlockState s) {
        return s.isAir() || !s.getFluidState().isEmpty()
                || s.is(BlockTags.DIRT) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.SAND)
                || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(BlockTags.SNOW) || s.is(BlockTags.ICE)
                || s.is(BlockTags.FLOWERS) || s.is(BlockTags.REPLACEABLE) || s.is(BlockTags.TERRACOTTA)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY);
    }
}
