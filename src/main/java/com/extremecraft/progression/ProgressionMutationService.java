package com.extremecraft.progression;

import com.extremecraft.progression.level.LevelService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical mutation facade for runtime progression state.
 *
 * <p>New gameplay code should mutate XP/level through this service so legacy capability
 * mirrors stay synchronized while canonical progression authority remains in one path.</p>
 */
public final class ProgressionMutationService {
    private ProgressionMutationService() {
    }

    public static int grantXp(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        // Canonical progression state.
        ProgressionService.addXp(player, amount);

        // Legacy mirrors kept in sync for existing systems/UI during migration.
        int levelUps = LevelService.grantLegacyXp(player, amount, false);
        PlayerStatsService.addExperience(player, amount, false);

        LevelService.sync(player);
        PlayerStatsService.sync(player);
        return levelUps;
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (player == null) {
            return;
        }

        int safeLevel = Math.max(1, level);

        // Canonical progression state.
        ProgressionService.setLevel(player, safeLevel);

        // Legacy mirrors kept in sync for existing systems/UI during migration.
        LevelService.setLegacyLevel(player, safeLevel, false);
        PlayerStatsService.setLevel(player, safeLevel, false);

        LevelService.sync(player);
        PlayerStatsService.sync(player);
    }
}
