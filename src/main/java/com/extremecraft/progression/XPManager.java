package com.extremecraft.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * Legacy compatibility wrapper for older XP callers.
 *
 * <p>New gameplay XP writes should enter through {@link ProgressionFacade}, with this type kept
 * only as a legacy adapter.</p>
 */
@Deprecated(forRemoval = false, since = "1.2.0")
public final class XPManager {
    private XPManager() {
    }

    public static void grant(ServerPlayer player, int amount) {
        if (amount <= 0) {
            return;
        }

        ProgressionFacade.grantPlayerXp(player, amount);
    }

    public static int xpRequiredForLevel(int level) {
        return LevelScaling.xpForLevel(level);
    }
}

