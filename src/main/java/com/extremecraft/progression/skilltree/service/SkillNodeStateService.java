package com.extremecraft.progression.skilltree.service;

import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;

/**
 * Computes UI state for a node from current stats and prerequisites.
 */
public final class SkillNodeStateService {
    public enum NodeState {
        LOCKED,
        UNLOCKABLE,
        UNLOCKED
    }

    private SkillNodeStateService() {
    }

    public static NodeState stateFor(PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return NodeState.LOCKED;
        }

        if (stats.isSkillUnlocked(node.id())) {
            return NodeState.UNLOCKED;
        }

        return SkillPrerequisiteEvaluator.canUnlock(stats, node)
                ? NodeState.UNLOCKABLE
                : NodeState.LOCKED;
    }
}
