package com.extremecraft.entity.system;

import com.extremecraft.config.Config;
import com.extremecraft.core.ECConstants;
import com.extremecraft.entity.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class BossArenaManager {
    private static final TagKey<Structure> ANCIENT_RESEARCH_LAB_ARENA = structureTag("ancient_research_lab_arena");
    private static final TagKey<Structure> VOID_TEMPLE_ARENA = structureTag("void_temple_arena");
    private static final TagKey<Structure> MACHINE_FORTRESS_ARENA = structureTag("machine_fortress_arena");

    private static final long STRUCTURE_MISS_CACHE_TTL_TICKS = 200L;
    private static final int STRUCTURE_MISS_CACHE_MAX_ENTRIES = 8192;
    private static final Map<String, Long> STRUCTURE_MISS_CACHE = new ConcurrentHashMap<>();

    private static final List<ArenaDefinition> ARENAS = List.of(
            new ArenaDefinition(
                    "ancient_research_lab",
                    ANCIENT_RESEARCH_LAB_ARENA,
                    () -> ModEntities.ANCIENT_CORE_GUARDIAN.get(),
                    () -> ModEntities.ANCIENT_SENTINEL.get(),
                    new ResourceLocation(ECConstants.MODID, "chests/ancient_research_lab"),
                    12
            ),
            new ArenaDefinition(
                    "void_temple",
                    VOID_TEMPLE_ARENA,
                    () -> ModEntities.VOID_TITAN.get(),
                    () -> ModEntities.VOID_STALKER.get(),
                    new ResourceLocation(ECConstants.MODID, "chests/void_temple"),
                    14
            ),
            new ArenaDefinition(
                    "machine_fortress",
                    MACHINE_FORTRESS_ARENA,
                    () -> ModEntities.OVERCHARGED_MACHINE_GOD.get(),
                    () -> ModEntities.TECH_CONSTRUCT.get(),
                    new ResourceLocation(ECConstants.MODID, "chests/machine_fortress"),
                    14
            )
    );

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!Config.isBossArenaSpawnsEnabled() || player.isSpectator()) {
            return;
        }

        int interval = Config.bossArenaCheckIntervalTicks();
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        pruneStructureMissCache(level.getGameTime());

        BossArenaState state = BossArenaState.get(level);
        for (ArenaDefinition arena : ARENAS) {
            attemptArenaSpawn(level, player, state, arena);
        }
    }

    private static void attemptArenaSpawn(ServerLevel level, ServerPlayer player, BossArenaState state, ArenaDefinition arena) {
        BlockPos playerPos = player.blockPosition();
        if (isArenaMissCached(level, arena, playerPos, level.getGameTime())) {
            return;
        }

        StructureStart start = level.structureManager().getStructureWithPieceAt(playerPos, arena.structureTag());
        if (!start.isValid()) {
            rememberArenaMiss(level, arena, playerPos, level.getGameTime());
            return;
        }

        BlockPos center = start.getBoundingBox().getCenter();
        if (playerPos.distSqr(center) > arena.triggerRadius() * arena.triggerRadius()) {
            return;
        }

        String arenaKey = buildArenaKey(level, arena.id(), center);
        if (state.hasTriggered(arenaKey)) {
            return;
        }

        EntityType<? extends Monster> bossType = arena.bossType().get();
        if (Config.isMobDisabled(bossType)) {
            return;
        }

        if (hasExistingBoss(level, center, bossType)) {
            state.markTriggered(arenaKey);
            return;
        }

        if (spawnBoss(level, start.getBoundingBox(), center, arena)) {
            state.markTriggered(arenaKey);
        }
    }

    private static boolean spawnBoss(ServerLevel level, BoundingBox box, BlockPos center, ArenaDefinition arena) {
        Monster boss = arena.bossType().get().create(level);
        if (boss == null) {
            return false;
        }

        BlockPos spawnPos = chooseSpawnPosition(level, box, center);
        prepareArenaBlocks(level, spawnPos, arena);

        boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);

        level.sendParticles(ParticleTypes.END_ROD, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                40, 0.8D, 0.9D, 0.8D, 0.04D);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.8F, 0.85F);
        return true;
    }

    private static void prepareArenaBlocks(ServerLevel level, BlockPos center, ArenaDefinition arena) {
        BlockPos chestPos = center.offset(2, 0, 0);
        BlockPos spawnerPos = center.offset(-2, 0, 0);

        if (isOpen(level, chestPos)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        }

        if (isOpen(level, spawnerPos)) {
            level.setBlock(spawnerPos, Blocks.SPAWNER.defaultBlockState(), 3);
        }

        BlockEntity chestEntity = level.getBlockEntity(chestPos);
        if (chestEntity instanceof ChestBlockEntity chest) {
            chest.setLootTable(arena.lootTable(), level.random.nextLong());
            chest.setChanged();
        }

        BlockEntity spawnerEntity = level.getBlockEntity(spawnerPos);
        if (spawnerEntity instanceof SpawnerBlockEntity spawner) {
            spawner.getSpawner().setEntityId(arena.spawnerEntity().get(), level, level.random, spawnerPos);
            spawner.setChanged();
        }
    }

    private static boolean hasExistingBoss(ServerLevel level, BlockPos center, EntityType<? extends Monster> bossType) {
        return !level.getEntitiesOfClass(Monster.class, new AABB(center).inflate(48.0D),
                entity -> entity.isAlive() && entity.getType() == bossType).isEmpty();
    }

    private static BlockPos chooseSpawnPosition(ServerLevel level, BoundingBox box, BlockPos center) {
        for (int y = box.maxY() + 2; y >= box.minY(); y--) {
            BlockPos candidate = new BlockPos(center.getX(), y, center.getZ());
            if (isClearSpawn(level, candidate)) {
                return candidate;
            }
        }

        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        return new BlockPos(center.getX(), surface + 1, center.getZ());
    }

    private static boolean isClearSpawn(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && !below.getCollisionShape(level, pos.below()).isEmpty();
    }

    private static boolean isOpen(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static TagKey<Structure> structureTag(String id) {
        return TagKey.create(Registries.STRUCTURE, new ResourceLocation(ECConstants.MODID, id));
    }

    private static String buildArenaKey(ServerLevel level, String id, BlockPos center) {
        return level.dimension().location() + "|" + id + "|" + center.asLong();
    }

    private static boolean isArenaMissCached(ServerLevel level, ArenaDefinition arena, BlockPos playerPos, long now) {
        String key = buildMissCacheKey(level, arena, playerPos);
        Long cachedTick = STRUCTURE_MISS_CACHE.get(key);
        return cachedTick != null && (now - cachedTick) <= STRUCTURE_MISS_CACHE_TTL_TICKS;
    }

    private static void rememberArenaMiss(ServerLevel level, ArenaDefinition arena, BlockPos playerPos, long now) {
        if (STRUCTURE_MISS_CACHE.size() >= STRUCTURE_MISS_CACHE_MAX_ENTRIES) {
            pruneStructureMissCache(now);
            if (STRUCTURE_MISS_CACHE.size() >= STRUCTURE_MISS_CACHE_MAX_ENTRIES) {
                STRUCTURE_MISS_CACHE.clear();
            }
        }

        STRUCTURE_MISS_CACHE.put(buildMissCacheKey(level, arena, playerPos), now);
    }

    private static String buildMissCacheKey(ServerLevel level, ArenaDefinition arena, BlockPos playerPos) {
        return level.dimension().location() + "|" + arena.id() + "|" + (playerPos.getX() >> 4) + "|" + (playerPos.getZ() >> 4);
    }

    private static void pruneStructureMissCache(long now) {
        STRUCTURE_MISS_CACHE.entrySet().removeIf(entry -> (now - entry.getValue()) > STRUCTURE_MISS_CACHE_TTL_TICKS);
    }

    private record ArenaDefinition(
            String id,
            TagKey<Structure> structureTag,
            Supplier<EntityType<? extends Monster>> bossType,
            Supplier<EntityType<? extends Monster>> spawnerEntity,
            ResourceLocation lootTable,
            int triggerRadius
    ) {
    }
}
