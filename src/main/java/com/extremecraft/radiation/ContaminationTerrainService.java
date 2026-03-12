package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ContaminationDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Terrain-facing extension of the canonical contamination pipeline.
 *
 * <p>Numeric chunk contamination remains the authority for dose and spread pressure. This
 * service translates that pressure into bounded terrain conversion pulses and exposes cleanup
 * hooks for future decontamination tools or machines.</p>
 */
public final class ContaminationTerrainService {
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

    public static void tickLevel(ServerLevel level) {
        if (level == null || !ECFoundationConfig.enableWorldEdits()) {
            return;
        }

        ContaminationDefinition profile = ChunkContaminationService.profile();
        if (profile.terrainVariants().isEmpty() || profile.terrainMutationsPerPulse() <= 0) {
            return;
        }

        long interval = Math.max(20L, ECFoundationConfig.contaminationDecayIntervalTicks() / 2L);
        if (level.getGameTime() % interval != 0L) {
            return;
        }

        int processed = 0;
        for (ServerPlayer player : level.players()) {
            if (processed++ >= 6) {
                break;
            }
            pulseAroundPlayer(level, player, profile);
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

    private static void pulseAroundPlayer(ServerLevel level, ServerPlayer player, ContaminationDefinition profile) {
        double contamination = ChunkContaminationService.getContamination(level, player.chunkPosition());
        BlockPos center = player.blockPosition();

        if (contamination >= profile.terrainMutationThreshold()) {
            for (int i = 0; i < profile.terrainMutationsPerPulse(); i++) {
                mutateRandomBlock(level, center, profile.terrainSpreadRadius(), profile, true);
            }
            return;
        }

        if (contamination > 0.0D && contamination <= (profile.terrainMutationThreshold() * 0.25D)) {
            recoverRandomBlock(level, center, profile.terrainSpreadRadius(), profile);
        }
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

    private static boolean recoverRandomBlock(ServerLevel level, BlockPos center, int radius, ContaminationDefinition profile) {
        RandomSource random = level.random;
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

            ContaminationDefinition.TerrainVariant variant = contaminatedVariants.get(blockId(level.getBlockState(target)));
            if (variant == null) {
                continue;
            }

            if (setVariantBlock(level, target, variant.cleanupBlockId())) {
                ChunkContaminationService.scrubContamination(level, new ChunkPos(target), Math.max(1.0D, profile.scrubRatePerPulse() * 0.5D));
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

        return level.setBlock(pos, nextState, Block.UPDATE_ALL_IMMEDIATE);
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
