package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ClassIdResolver;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical live write path for class XP.
 *
 * <p>Class XP is intentionally constrained to guild quest reward flow (plus debug/admin tools)
 * so unrelated gameplay systems do not create side-door class progression drift.</p>
 */
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
            ProgressionSyncService.flush(player);
        } else {
            ProgressApi.get(player).ifPresent(PlayerProgressData::markSyncDirty);
            ProgressionSyncService.flush(player);
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
        String normalized = ClassIdResolver.normalizeCanonical(classId);
        if (!normalized.isBlank()) {
            return normalized;
        }
        return ClassIdResolver.normalizeCanonical(data.currentClass());
    }
}
