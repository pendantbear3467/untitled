package com.extremecraft.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical mutation facade for runtime progression state.
 *
 * <p>New gameplay code should mutate XP/level through this service so legacy capability
 * mirrors stay synchronized while canonical progression authority remains in one path.</p>
 */
final class ProgressionMutationService {
    private ProgressionMutationService() {
    }

    static int grantXp(ServerPlayer player, int amount) {
        ProgressionMutationAuthority.warnIfBypassed("grantXp");
        if (player == null || amount <= 0) {
            return 0;
        }

        // Canonical progression state.
        ProgressionService.addXp(player, amount);

        // Legacy mirrors kept in sync for existing systems/UI during migration.
        int levelUps = com.extremecraft.progression.level.LevelService.grantLegacyXp(player, amount, false);
        PlayerStatsService.addExperience(player, amount, false);
        PlayerStatsService.syncProgressionMirror(player, false);

        // Push sync after both canonical + mirror mutations so clients never see mixed state.
        ProgressionSyncService.syncLegacyMirrors(player);
        return levelUps;
    }

    static void setLevel(ServerPlayer player, int level) {
        ProgressionMutationAuthority.warnIfBypassed("setLevel");
        if (player == null) {
            return;
        }

        int safeLevel = Math.max(1, level);

        // Canonical progression state.
        ProgressionService.setLevel(player, safeLevel);

        // Legacy mirrors kept in sync for existing systems/UI during migration.
        com.extremecraft.progression.level.LevelService.setLegacyLevel(player, safeLevel, false);
        PlayerStatsService.setLevel(player, safeLevel, false);
        PlayerStatsService.syncProgressionMirror(player, false);

        // Push sync after both canonical + mirror mutations so clients never see mixed state.
        ProgressionSyncService.syncLegacyMirrors(player);
    }
}
