package com.extremecraft.ecosystem.core.progression;

import net.minecraft.server.level.ServerPlayer;

/**
 * Core-level read-only contract for progression state access.
 *
 * <p>Compat/domain modules should query progression through this interface and avoid
 * direct capability mutation or low-level progression service access.</p>
 */
public interface ProgressionReadAccess {
    int level(ServerPlayer player);

    int xp(ServerPlayer player);

    int playerSkillPoints(ServerPlayer player);

    int classSkillPoints(ServerPlayer player);

    int questProgress(ServerPlayer player, String questId);

    boolean questCompleted(ServerPlayer player, String questId);

    String currentClass(ServerPlayer player);

    int classLevel(ServerPlayer player, String classId);

    int classXp(ServerPlayer player, String classId);

    boolean hasUnlock(ServerPlayer player, String unlockId);
}
