package com.extremecraft.entity.system;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.function.Supplier;

public final class BossArenaManager {
    private static final TagKey<Structure> ANCIENT_RESEARCH_LAB_ARENA = structureTag("ancient_research_lab_arena");
    private static final TagKey<Structure> VOID_TEMPLE_ARENA = structureTag("void_temple_arena");
    private static final TagKey<Structure> MACHINE_FORTRESS_ARENA = structureTag("machine_fortress_arena");

    private static final List<ArenaDefinition> ARENAS = List.of(
            new ArenaDefinition("ancient_research_lab", ANCIENT_RESEARCH_LAB_ARENA, () -> ModEntities.ANCIENT_CORE_GUARDIAN.get(), 12),
            new ArenaDefinition("void_temple", VOID_TEMPLE_ARENA, () -> ModEntities.VOID_TITAN.get(), 14),
            new ArenaDefinition("machine_fortress", MACHINE_FORTRESS_ARENA, () -> ModEntities.OVERCHARGED_MACHINE_GOD.get(), 14)
    );

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 40 != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        BossArenaState state = BossArenaState.get(level);

        for (ArenaDefinition arena : ARENAS) {
            attemptArenaSpawn(level, player, state, arena);
        }
    }

    private static void attemptArenaSpawn(ServerLevel level, ServerPlayer player, BossArenaState state, ArenaDefinition arena) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(player.blockPosition(), arena.structureTag());
        if (!start.isValid()) {
            return;
        }

        BlockPos center = start.getBoundingBox().getCenter();
        if (player.blockPosition().distSqr(center) > arena.triggerRadius() * arena.triggerRadius()) {
            return;
        }

        String arenaKey = buildArenaKey(level, arena.id(), center);
        if (state.hasTriggered(arenaKey)) {
            return;
        }

        EntityType<? extends Monster> bossType = arena.bossType().get();
        if (hasExistingBoss(level, center, bossType)) {
            state.markTriggered(arenaKey);
            return;
        }

        if (spawnBoss(level, start.getBoundingBox(), center, bossType)) {
            state.markTriggered(arenaKey);
        }
    }

    private static boolean spawnBoss(ServerLevel level, BoundingBox box, BlockPos center, EntityType<? extends Monster> bossType) {
        Monster boss = bossType.create(level);
        if (boss == null) {
            return false;
        }

        BlockPos spawnPos = chooseSpawnPosition(level, box, center);
        boss.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
        boss.setPersistenceRequired();
        level.addFreshEntity(boss);

        level.sendParticles(ParticleTypes.END_ROD, spawnPos.getX() + 0.5D, spawnPos.getY() + 1.0D, spawnPos.getZ() + 0.5D,
                40, 0.8D, 0.9D, 0.8D, 0.04D);
        level.playSound(null, spawnPos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.8F, 0.85F);
        return true;
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

    private static TagKey<Structure> structureTag(String id) {
        return TagKey.create(Registries.STRUCTURE, new ResourceLocation(ECConstants.MODID, id));
    }

    private static String buildArenaKey(ServerLevel level, String id, BlockPos center) {
        return level.dimension().location() + "|" + id + "|" + center.asLong();
    }

    private record ArenaDefinition(String id, TagKey<Structure> structureTag, Supplier<EntityType<? extends Monster>> bossType,
                                   int triggerRadius) {
    }
}
