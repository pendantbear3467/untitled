package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ContaminationDefinition;
import com.extremecraft.platform.data.definition.ContaminationTerrainDefinition;
import com.extremecraft.platform.data.registry.ContaminationTerrainDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Terrain-facing extension of the canonical contamination pipeline.
 *
 * <p>Numeric chunk contamination remains the authority for dose and spread pressure. This
 * service translates that pressure into bounded terrain conversion pulses and exposes cleanup
 * hooks for future decontamination tools or machines.</p>
 */
public final class ContaminationTerrainService {
    private static final int DEFAULT_MAX_CHUNKS_PER_PULSE = 8;
    private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 3;
    private static final Direction[] SPREAD_DIRECTIONS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN
    };

    private ContaminationTerrainService() {
    }

    public static void seedRelease(ServerLevel level, BlockPos center, double amount, int radius) {
        if (level == null || center == null || amount <= 0.0D || !ECFoundationConfig.enableWorldEdits()) {
            return;
        }

        ContaminationDefinition profile = ChunkContaminationService.profile();
        if (profile.terrainVariants().isEmpty() || profile.terrainMutationsPerPulse() <= 0) {
            return;
        }

        int attempts = Math.max(profile.terrainMutationsPerPulse(), Math.min(24, Math.max(1, radius * 2)));
        for (int i = 0; i < attempts; i++) {
            mutateRandomBlock(level, center, Math.max(2, radius), profile, false);
        }
    }

    /**
     * Applies the canonical periodic terrain mutation pulse for contaminated chunks.
     *
     * <p>Numeric chunk contamination remains the single runtime authority. These rules are a
     * visual/environmental projection of that state and should be fed only from
     * {@link ChunkContaminationService}.</p>
     */
    public static void tickLevel(ServerLevel level, Map<Long, Double> contaminationByChunk) {
        if (level == null || contaminationByChunk == null || contaminationByChunk.isEmpty() || !ECFoundationConfig.enableWorldEdits()) {
            return;
        }

        List<ContaminationTerrainDefinition> rules = new ArrayList<>(ContaminationTerrainDataRegistry.registry().all());
        if (rules.isEmpty()) {
            return;
        }

        int maxChunks = Math.max(1, Math.min(DEFAULT_MAX_CHUNKS_PER_PULSE, ECFoundationConfig.catastrophicMaxAffectedBlocks() / 16));
        int visitedChunks = 0;
        RandomSource random = level.random;

        for (Map.Entry<Long, Double> entry : contaminationByChunk.entrySet()) {
            if (visitedChunks >= maxChunks) {
                break;
            }

            double contamination = entry.getValue();
            if (contamination <= 0.0D) {
                continue;
            }

            visitedChunks++;
            applyChunk(level, new ChunkPos(entry.getKey()), contamination, rules, random);
        }
    }

    public static void handleContaminatedBlockBreak(ServerLevel level, BlockPos pos, BlockState state, ServerPlayer player) {
        if (level == null || pos == null || state == null || player == null) {
            return;
        }

        ContaminationDefinition profile = ChunkContaminationService.profile();
        ContaminationDefinition.TerrainVariant variant = contaminatedVariantByBlock(profile).get(blockId(state));
        if (variant == null) {
            return;
        }

        double cleanup = Math.max(0.0D, profile.scrubRatePerPulse() * RadiationProtectionService.cleanupEfficiency(player));
        if (cleanup > 0.0D) {
            ChunkContaminationService.scrubContamination(level, new ChunkPos(pos), cleanup);
        }
    }

    public static double scrubArea(ServerLevel level, BlockPos center, int radius, double amountPerChunk) {
        if (level == null || center == null || radius < 0 || amountPerChunk <= 0.0D) {
            return 0.0D;
        }

        double removed = 0.0D;
        int chunkRadius = Math.max(0, radius >> 4);
        ChunkPos origin = new ChunkPos(center);
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                removed += ChunkContaminationService.scrubContamination(level, new ChunkPos(origin.x + x, origin.z + z), amountPerChunk);
            }
        }
        return removed;
    }

    private static boolean applyChunk(ServerLevel level,
                                      ChunkPos chunkPos,
                                      double contamination,
                                      List<ContaminationTerrainDefinition> rules,
                                      RandomSource random) {
        List<ContaminationTerrainDefinition> applicable = rules.stream()
                .filter(rule -> contamination >= rule.minChunkContamination())
                .toList();
        if (applicable.isEmpty()) {
            return false;
        }

        for (int attempt = 0; attempt < DEFAULT_ATTEMPTS_PER_CHUNK; attempt++) {
            int x = chunkPos.getMinBlockX() + random.nextInt(16);
            int z = chunkPos.getMinBlockZ() + random.nextInt(16);
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            if (surfaceY < level.getMinBuildHeight()) {
                continue;
            }

            BlockPos targetPos = new BlockPos(x, surfaceY, z);
            BlockState currentState = level.getBlockState(targetPos);
            ResourceLocation currentId = BuiltInRegistries.BLOCK.getKey(currentState.getBlock());
            if (currentId == null) {
                continue;
            }

            for (ContaminationTerrainDefinition rule : applicable) {
                if (!currentId.toString().equals(rule.sourceBlockId()) || random.nextDouble() > rule.chancePerPulse()) {
                    continue;
                }

                Block replacement = blockById(rule.resultBlockId());
                if (replacement == null) {
                    continue;
                }

                BlockState replacementState = replacement.defaultBlockState();
                if (replacement == Blocks.AIR && currentState.isAir()) {
                    continue;
                }
                if (replacementState.equals(currentState)) {
                    continue;
                }

                if (level.setBlock(targetPos, replacementState, Block.UPDATE_ALL)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean mutateRandomBlock(ServerLevel level, BlockPos center, int radius, ContaminationDefinition profile, boolean allowSpreadFromVariant) {
        RandomSource random = level.random;
        Map<String, ContaminationDefinition.TerrainVariant> sourceVariants = sourceVariantByBlock(profile);
        Map<String, ContaminationDefinition.TerrainVariant> contaminatedVariants = contaminatedVariantByBlock(profile);

        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos target = center.offset(
                    random.nextInt((radius * 2) + 1) - radius,
                    random.nextInt(5) - 2,
                    random.nextInt((radius * 2) + 1) - radius
            );
            if (!level.isLoaded(target)) {
                continue;
            }

            BlockState state = level.getBlockState(target);
            ContaminationDefinition.TerrainVariant direct = sourceVariants.get(blockId(state));
            if (direct != null && setVariantBlock(level, target, direct.contaminatedBlockId())) {
                return true;
            }

            if (!allowSpreadFromVariant) {
                continue;
            }

            ContaminationDefinition.TerrainVariant contaminated = contaminatedVariants.get(blockId(state));
            if (contaminated == null) {
                continue;
            }

            Direction direction = SPREAD_DIRECTIONS[random.nextInt(SPREAD_DIRECTIONS.length)];
            BlockPos neighbor = target.relative(direction);
            if (!level.isLoaded(neighbor)) {
                continue;
            }

            ContaminationDefinition.TerrainVariant neighborVariant = sourceVariants.get(blockId(level.getBlockState(neighbor)));
            if (neighborVariant != null && setVariantBlock(level, neighbor, neighborVariant.contaminatedBlockId())) {
                return true;
            }
        }

        return false;
    }

    private static boolean setVariantBlock(ServerLevel level, BlockPos pos, String blockId) {
        Block block = blockById(blockId);
        if (block == null) {
            return false;
        }

        BlockState nextState = block.defaultBlockState();
        if (level.getBlockState(pos).is(nextState.getBlock())) {
            return false;
        }

        return level.setBlock(pos, nextState, Block.UPDATE_ALL);
    }

    private static Map<String, ContaminationDefinition.TerrainVariant> sourceVariantByBlock(ContaminationDefinition profile) {
        Map<String, ContaminationDefinition.TerrainVariant> variants = new LinkedHashMap<>();
        for (ContaminationDefinition.TerrainVariant variant : profile.terrainVariants()) {
            variants.putIfAbsent(variant.sourceBlockId(), variant);
        }
        return variants;
    }

    private static Map<String, ContaminationDefinition.TerrainVariant> contaminatedVariantByBlock(ContaminationDefinition profile) {
        Map<String, ContaminationDefinition.TerrainVariant> variants = new LinkedHashMap<>();
        for (ContaminationDefinition.TerrainVariant variant : profile.terrainVariants()) {
            variants.putIfAbsent(variant.contaminatedBlockId(), variant);
        }
        return variants;
    }

    private static String blockId(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.toString();
    }

    private static Block blockById(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }

        Block block = BuiltInRegistries.BLOCK.get(key);
        return block == null ? null : block;
    }
}
