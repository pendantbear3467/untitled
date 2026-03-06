package com.extremecraft.entity;

import com.extremecraft.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public final class MobSpawns {
    private static boolean bootstrapped;

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }

        register(ModEntities.TECH_CONSTRUCT.get());
        register(ModEntities.ARCANE_WRAITH.get());
        register(ModEntities.VOID_STALKER.get());
        register(ModEntities.ANCIENT_SENTINEL.get());
        register(ModEntities.ENERGY_PARASITE.get());
        register(ModEntities.RUNIC_GOLEM.get());

        register(ModEntities.ANCIENT_CORE_GUARDIAN.get());
        register(ModEntities.VOID_TITAN.get());
        register(ModEntities.OVERCHARGED_MACHINE_GOD.get());

        bootstrapped = true;
    }

    private static <T extends Monster> void register(EntityType<T> entityType) {
        SpawnPlacements.register(
                entityType,
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (type, level, spawnType, pos, random) -> canSpawn(type, level, spawnType, pos, random)
        );
    }

    private static <T extends Monster> boolean canSpawn(EntityType<T> entityType,
                                                        ServerLevelAccessor level,
                                                        MobSpawnType spawnType,
                                                        BlockPos pos,
                                                        RandomSource random) {
        if (Config.isMobDisabled(entityType)) {
            return false;
        }

        if (!Monster.checkMonsterSpawnRules(entityType, level, spawnType, pos, random)) {
            return false;
        }

        if (spawnType != MobSpawnType.NATURAL && spawnType != MobSpawnType.CHUNK_GENERATION) {
            return true;
        }

        if (!Config.isNaturalMobSpawningEnabledFor(entityType)) {
            return false;
        }

        int maxNearby = Math.max(0, Config.COMMON.mobs.maxNearbySameMob.get());
        if (maxNearby <= 0) {
            return true;
        }

        double radius = Math.max(4.0D, Config.COMMON.mobs.nearbyMobCheckRadius.get());
        int nearbyCount = level.getLevel().getEntitiesOfClass(
                Monster.class,
                new AABB(pos).inflate(radius),
                mob -> mob.isAlive() && mob.getType() == entityType
        ).size();

        return nearbyCount < maxNearby;
    }

    private MobSpawns() {
    }
}
