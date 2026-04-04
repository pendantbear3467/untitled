package com.extremecraft.progression.skilltree.service;

import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import net.minecraft.world.entity.player.Player;

/**
 * Evaluates whether a node can be unlocked from current player stats.
 */
public final class SkillPrerequisiteEvaluator {
    private SkillPrerequisiteEvaluator() {
    }

    public static boolean canUnlock(PlayerStatsCapability stats, SkillNode node) {
        return canUnlock(null, stats, node);
    }

    public static boolean canUnlock(Player player, PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return false;
        }

        if (stats.isSkillUnlocked(node.id())) {
            return false;
        }

        int level = ProgressApi.get(player)
                .map(PlayerProgressData::level)
                .orElse(stats.level());
        int playerSkillPoints = ProgressApi.get(player)
                .map(PlayerProgressData::playerSkillPoints)
                .orElse(stats.skillPoints());
        if (playerSkillPoints < node.cost() || level < node.requiredLevel()) {
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
