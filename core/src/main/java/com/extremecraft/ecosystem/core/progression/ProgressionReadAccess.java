package com.extremecraft.ecosystem.core.progression;

/**
 * Core-level read-only contract for progression state access.
 */
public interface ProgressionReadAccess {
    int level(Object player);

    int xp(Object player);

    int playerSkillPoints(Object player);

    int classSkillPoints(Object player);

    int questProgress(Object player, String questId);

    boolean questCompleted(Object player, String questId);

    String currentClass(Object player);

    int classLevel(Object player, String classId);

    int classXp(Object player, String classId);

    boolean hasUnlock(Object player, String unlockId);
}
