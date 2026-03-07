package com.extremecraft.machine.sync;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime index of machine sync providers keyed by chunk for efficient nearby sync queries.
 */
public final class MachineSyncIndex {
    private static final Map<net.minecraft.resources.ResourceKey<Level>, Map<Long, Set<Long>>> BY_DIMENSION = new ConcurrentHashMap<>();

    private MachineSyncIndex() {
    }

    public static void register(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || pos == null) {
            return;
        }

        Map<Long, Set<Long>> byChunk = BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>());
        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        byChunk.computeIfAbsent(chunkKey, ignored -> ConcurrentHashMap.newKeySet()).add(pos.asLong());
    }

    public static void unregister(Level level, BlockPos pos) {
        if (level == null || level.isClientSide || pos == null) {
            return;
        }

        Map<Long, Set<Long>> byChunk = BY_DIMENSION.get(level.dimension());
        if (byChunk == null) {
            return;
        }

        long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        Set<Long> positions = byChunk.get(chunkKey);
        if (positions == null) {
            return;
        }

        positions.remove(pos.asLong());
        if (positions.isEmpty()) {
            byChunk.remove(chunkKey, positions);
        }

        if (byChunk.isEmpty()) {
            BY_DIMENSION.remove(level.dimension(), byChunk);
        }
    }

    public static List<BlockPos> collectNearby(ServerPlayer player, int horizontalRadius, int verticalRadius, int maxCount) {
        if (player == null || maxCount <= 0) {
            return List.of();
        }

        Level level = player.level();
        if (level.isClientSide) {
            return List.of();
        }

        Map<Long, Set<Long>> byChunk = BY_DIMENSION.get(level.dimension());
        if (byChunk == null || byChunk.isEmpty()) {
            return List.of();
        }

        BlockPos center = player.blockPosition();
        int centerX = center.getX();
        int centerY = center.getY();
        int centerZ = center.getZ();

        int radiusXZ = Math.max(0, horizontalRadius);
        int radiusY = Math.max(0, verticalRadius);

        int minChunkX = (centerX - radiusXZ) >> 4;
        int maxChunkX = (centerX + radiusXZ) >> 4;
        int minChunkZ = (centerZ - radiusXZ) >> 4;
        int maxChunkZ = (centerZ + radiusXZ) >> 4;

        List<BlockPos> candidates = new ArrayList<>();
        List<Long> stale = new ArrayList<>();

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                long chunkKey = ChunkPos.asLong(chunkX, chunkZ);
                Set<Long> tracked = byChunk.get(chunkKey);
                if (tracked == null || tracked.isEmpty()) {
                    continue;
                }

                for (long packedPos : tracked) {
                    BlockPos pos = BlockPos.of(packedPos);
                    if (Math.abs(pos.getX() - centerX) > radiusXZ
                            || Math.abs(pos.getY() - centerY) > radiusY
                            || Math.abs(pos.getZ() - centerZ) > radiusXZ) {
                        continue;
                    }

                    if (!level.isLoaded(pos)) {
                        continue;
                    }

                    BlockEntity blockEntity = level.getBlockEntity(pos);
                    if (!(blockEntity instanceof MachineStateSyncProvider) || blockEntity.isRemoved()) {
                        stale.add(packedPos);
                        continue;
                    }

                    candidates.add(pos.immutable());
                }
            }
        }

        pruneStale(level, byChunk, stale);

        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator
                .comparingDouble((BlockPos pos) -> pos.distSqr(center))
                .thenComparingLong(BlockPos::asLong));

        if (candidates.size() > maxCount) {
            return List.copyOf(candidates.subList(0, maxCount));
        }

        return List.copyOf(candidates);
    }

    public static void clear() {
        BY_DIMENSION.clear();
    }

    private static void pruneStale(Level level, Map<Long, Set<Long>> byChunk, List<Long> stale) {
        if (stale.isEmpty()) {
            return;
        }

        for (long packedPos : stale) {
            BlockPos pos = BlockPos.of(packedPos);
            long chunkKey = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
            Set<Long> tracked = byChunk.get(chunkKey);
            if (tracked == null) {
                continue;
            }

            tracked.remove(packedPos);
            if (tracked.isEmpty()) {
                byChunk.remove(chunkKey, tracked);
            }
        }

        if (byChunk.isEmpty()) {
            BY_DIMENSION.remove(level.dimension(), byChunk);
        }
    }
}