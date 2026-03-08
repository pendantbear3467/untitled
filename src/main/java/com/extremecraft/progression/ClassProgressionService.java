package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;

public final class ClassProgressionService {
    public enum Source {
        GUILD_QUEST,
        DEBUG_COMMAND
    }

    private ClassProgressionService() {
    }

    public static int grantClassXp(ServerPlayer player, String classId, int amount, Source source) {
        if (player == null || amount <= 0 || source == null) {
            return 0;
        }

        if (source != Source.GUILD_QUEST && source != Source.DEBUG_COMMAND) {
            return 0;
        }

        int gainedLevels = ProgressApi.get(player).map(data -> {
            String resolvedClassId = resolveClassId(data, classId);
            return resolvedClassId.isBlank() ? 0 : data.addClassExperience(resolvedClassId, amount);
        }).orElse(0);

        if (gainedLevels > 0) {
            ProgressionService.flushDirty(player);
        } else {
            ProgressApi.get(player).ifPresent(PlayerProgressData::markSyncDirty);
            ProgressionService.flushDirty(player);
        }
        return gainedLevels;
    }

    public static int currentClassXp(ServerPlayer player) {
        if (player == null) {
            return 0;
        }

        return ProgressApi.get(player)
                .map(data -> data.getClassExperience(data.currentClass()))
                .orElse(0);
    }

    public static int currentClassLevel(ServerPlayer player) {
        if (player == null) {
            return 1;
        }

        return ProgressApi.get(player)
                .map(data -> data.getClassLevel(data.currentClass()))
                .orElse(1);
    }

    private static String resolveClassId(PlayerProgressData data, String classId) {
        String normalized = classId == null ? "" : classId.trim().toLowerCase();
        if (!normalized.isBlank()) {
            return normalized;
        }
        return data.currentClass() == null ? "" : data.currentClass().trim().toLowerCase();
    }
}
