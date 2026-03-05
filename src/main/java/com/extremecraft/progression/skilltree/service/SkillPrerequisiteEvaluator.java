package com.extremecraft.progression.skilltree.service;

import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;

/**
 * Evaluates whether a node can be unlocked from current player stats.
 */
public final class SkillPrerequisiteEvaluator {
    private SkillPrerequisiteEvaluator() {
    }

    public static boolean canUnlock(PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return false;
        }

        if (stats.isSkillUnlocked(node.id())) {
            return false;
        }

        if (stats.skillPoints() < node.cost() || stats.level() < node.requiredLevel()) {
            return false;
        }

        for (String required : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(required)) {
                return false;
            }
        }

        return true;
    }
}
