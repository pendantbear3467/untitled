package com.extremecraft.entity;

import com.extremecraft.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MobSpawns {
    private static final Map<String, DensitySnapshot> DENSITY_CACHE = new ConcurrentHashMap<>();
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
        int nearbyCount = queryNearbyCount(level, entityType, pos, radius);
        return nearbyCount < maxNearby;
    }

    private static int queryNearbyCount(ServerLevelAccessor level, EntityType<?> entityType, BlockPos pos, double radius) {
        long now = level.getLevel().getGameTime();
        String key = cacheKey(level, entityType, pos);

        DensitySnapshot cached = DENSITY_CACHE.get(key);
        if (cached != null && (now - cached.tick()) <= 10L) {
            return cached.count();
        }

        int nearbyCount = level.getLevel().getEntitiesOfClass(
                Monster.class,
                new AABB(pos).inflate(radius),
                mob -> mob.isAlive() && mob.getType() == entityType
        ).size();

        DENSITY_CACHE.put(key, new DensitySnapshot(now, nearbyCount));
        return nearbyCount;
    }

    private static String cacheKey(ServerLevelAccessor level, EntityType<?> entityType, BlockPos pos) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entityType);
        String id = entityId == null ? "unknown" : entityId.toString();
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return level.getLevel().dimension().location() + "|" + id + "|" + chunkX + "|" + chunkZ;
    }

    private MobSpawns() {
    }

    private record DensitySnapshot(long tick, int count) {
    }
}
