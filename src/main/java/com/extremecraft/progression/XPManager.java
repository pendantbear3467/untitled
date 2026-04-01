package com.extremecraft.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * Legacy compatibility wrapper for older XP callers.
 *
 * <p>New gameplay XP writes should enter through {@code ProgressionEvents},
 * {@code GuildQuestRewardService}, or debug/admin commands, all of which converge on
 * {@code ProgressionMutationService}.</p>
 */
public final class XPManager {
    private XPManager() {
    }

    public static void grant(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        ProgressionMutationService.grantXp(player, amount);
    }

    public static int xpRequiredForLevel(int level) {
        return LevelScaling.xpForLevel(level);
    }
}

