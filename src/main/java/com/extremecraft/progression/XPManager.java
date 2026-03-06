package com.extremecraft.progression;

import net.minecraft.server.level.ServerPlayer;

public final class XPManager {
    private XPManager() {
    }

    public static void grant(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        PlayerStatsService.addExperience(player, amount);
        ProgressionService.addXp(player, amount);
    }

    public static int xpRequiredForLevel(int level) {
        return LevelScaling.xpForLevel(level);
    }
}
