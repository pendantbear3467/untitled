package com.extremecraft.progression.skilltree.service;

import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import net.minecraft.world.entity.player.Player;

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
        return stateFor(null, stats, node);
    }

    public static NodeState stateFor(Player player, PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return NodeState.LOCKED;
        }

        if (stats.isSkillUnlocked(node.id())) {
            return NodeState.UNLOCKED;
        }

        return SkillPrerequisiteEvaluator.canUnlock(player, stats, node)
                ? NodeState.UNLOCKABLE
                : NodeState.LOCKED;
    }
}
