package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.level.LevelService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical progression sync coordinator.
 */
public final class ProgressionSyncService {
    private ProgressionSyncService() {
    }

    public static void flush(ServerPlayer player) {
        ProgressionService.flushDirty(player);
    }

    public static void markCanonicalSyncDirty(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(PlayerProgressData::markSyncDirty);
    }

    public static void markCanonicalAttributesDirty(ServerPlayer player) {
        ProgressApi.get(player).ifPresent(PlayerProgressData::markAttributesDirty);
    }

    public static void syncCanonical(ServerPlayer player) {
        ProgressionService.sync(player);
    }

    public static void syncLegacyMirrors(ServerPlayer player) {
        LevelService.sync(player);
        PlayerStatsService.sync(player);
    }

    public static void syncAll(ServerPlayer player) {
        flush(player);
        syncLegacyMirrors(player);
    }
}
