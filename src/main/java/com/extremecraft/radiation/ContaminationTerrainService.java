package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ContaminationTerrainDefinition;
import com.extremecraft.platform.data.registry.ContaminationTerrainDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minimal terrain fallout extension layered onto chunk contamination.
 */
public final class ContaminationTerrainService {
    private static final int DEFAULT_MAX_CHUNKS_PER_PULSE = 8;
    private static final int DEFAULT_ATTEMPTS_PER_CHUNK = 3;

    private ContaminationTerrainService() {
    }

    public static void tickLevel(ServerLevel level, Map<Long, Double> contaminationByChunk) {
        if (level == null || contaminationByChunk == null || contaminationByChunk.isEmpty() || !ECFoundationConfig.enableWorldEdits()) {
            return;
        }

        List<ContaminationTerrainDefinition> rules = new ArrayList<>(ContaminationTerrainDataRegistry.registry().all());
        if (rules.isEmpty()) {
            return;
        }

        int maxChunks = Math.max(1, Math.min(DEFAULT_MAX_CHUNKS_PER_PULSE, ECFoundationConfig.catastrophicMaxAffectedBlocks() / 16));
        int processedChunks = 0;
        RandomSource random = level.random;

        for (Map.Entry<Long, Double> entry : contaminationByChunk.entrySet()) {
            if (processedChunks >= maxChunks) {
                break;
            }

            double contamination = entry.getValue();
            if (contamination <= 0.0D) {
                continue;
            }

            ChunkPos chunkPos = new ChunkPos(entry.getKey());
            if (applyChunk(level, chunkPos, contamination, rules, random)) {
                processedChunks++;
            }
        }
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
            ResourceLocation currentId = ForgeRegistries.BLOCKS.getKey(currentState.getBlock());
            if (currentId == null) {
                continue;
            }

            for (ContaminationTerrainDefinition rule : applicable) {
                if (!currentId.toString().equals(rule.sourceBlockId()) || random.nextDouble() > rule.chancePerPulse()) {
                    continue;
                }

                Block replacement = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(rule.resultBlockId()));
                if (replacement == null) {
                    continue;
                }

                BlockState replacementState = replacement.defaultBlockState();
                if (replacement == Blocks.AIR && currentState.isAir()) {
                    continue;
                }
                if (replacementState == currentState) {
                    continue;
                }

                level.setBlock(targetPos, replacementState, 3);
                return true;
            }
        }

        return false;
    }
}