package com.extremecraft.radiation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ContaminationDefinition;
import com.extremecraft.platform.data.registry.ContaminationDataRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public final class ChunkContaminationService {
    private static final String DATA_NAME = "extremecraft_chunk_contamination";

    private ChunkContaminationService() {
    }

    public static double getContamination(ServerLevel level, ChunkPos pos) {
        return data(level).contamination.getOrDefault(pos.toLong(), 0.0D);
    }

    public static void addContamination(ServerLevel level, ChunkPos pos, double amount) {
        if (level == null || pos == null || amount <= 0.0D) {
            return;
        }

        ContaminationDefinition profile = profile();
        ContaminationData data = data(level);
        long key = pos.toLong();
        double next = Math.min(profile.maxChunkContamination(), data.contamination.getOrDefault(key, 0.0D) + amount);
        data.contamination.put(key, next);
        data.setDirty();
    }

    public static double scrubContamination(ServerLevel level, ChunkPos pos, double amount) {
        if (level == null || pos == null || amount <= 0.0D) {
            return 0.0D;
        }

        ContaminationData data = data(level);
        long key = pos.toLong();
        double current = data.contamination.getOrDefault(key, 0.0D);
        if (current <= 0.0D) {
            return 0.0D;
        }

        double removed = Math.min(current, amount);
        double next = current - removed;
        if (next <= 0.0D) {
            data.contamination.remove(key);
        } else {
            data.contamination.put(key, next);
        }
        data.setDirty();
        return removed;
    }

    public static void releaseArea(ServerLevel level, net.minecraft.core.BlockPos center, double amount, int radius) {
        if (level == null || center == null || amount <= 0.0D) {
            return;
        }

        int chunkRadius = Math.max(0, radius >> 4);
        ChunkPos origin = new ChunkPos(center);
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos target = new ChunkPos(origin.x + x, origin.z + z);
                addContamination(level, target, amount / Math.max(1.0D, Math.sqrt((x * x) + (z * z) + 1.0D)));
            }
        }
    }

    public static void tickLevel(ServerLevel level) {
        if (level.getGameTime() % Math.max(20, ECFoundationConfig.contaminationDecayIntervalTicks()) != 0L) {
            return;
        }

        ContaminationDefinition profile = profile();
        ContaminationData data = data(level);
        data.contamination.entrySet().removeIf(entry -> {
            double next = Math.max(0.0D, entry.getValue() - profile.naturalDecayPerPulse() - ECFoundationConfig.contaminationDecayAmount());
            if (next <= 0.0D) {
                data.setDirty();
                return true;
            }
            entry.setValue(next);
            data.setDirty();
            return false;
        });

        ContaminationTerrainService.tickLevel(level, data.contamination);
    }

    public static ContaminationDefinition profile() {
        for (ContaminationDefinition definition : ContaminationDataRegistry.registry().all()) {
            return definition;
        }
        return new ContaminationDefinition("default", 10_000.0D, 0.45D, 8.0D, 0.2D, 25.0D, 35.0D, 0, 6, java.util.List.of());
    }

    private static ContaminationData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ContaminationData::load, ContaminationData::new, DATA_NAME);
    }

    private static final class ContaminationData extends SavedData {
        private final Map<Long, Double> contamination = new HashMap<>();

        private static ContaminationData load(CompoundTag tag) {
            ContaminationData data = new ContaminationData();
            ListTag list = tag.getList("chunks", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                if (!(element instanceof CompoundTag chunkTag)) {
                    continue;
                }
                data.contamination.put(chunkTag.getLong("chunk"), chunkTag.getDouble("value"));
            }
            return data;
        }

        @Override
        public CompoundTag save(@Nonnull CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, Double> entry : contamination.entrySet()) {
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putLong("chunk", entry.getKey());
                chunkTag.putDouble("value", entry.getValue());
                list.add(chunkTag);
            }
            tag.put("chunks", list);
            return tag;
        }
    }
}
