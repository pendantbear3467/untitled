package com.extremecraft.classsystem;

import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.server.level.ServerPlayer;

/**
 * Runtime gate for class unlock requirements.
 */
public final class ClassRequirements {
    private ClassRequirements() {
    }

    /**
     * Resolves player level from progression capabilities and checks against class requirement.
     */
    public static boolean canUseClass(ServerPlayer player, PlayerClass playerClass) {
        if (player == null || playerClass == null) {
            return false;
        }

        int playerLevel = ProgressApi.get(player)
                .map(data -> data.level())
                .or(() -> PlayerStatsApi.get(player).map(stats -> stats.level()))
                .orElse(1);
        return playerLevel >= playerClass.requiredLevel();
    }
}
