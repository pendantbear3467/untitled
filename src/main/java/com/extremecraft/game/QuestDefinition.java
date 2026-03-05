package com.extremecraft.game;

public record QuestDefinition(
        String id,
        String title,
        QuestType type,
        int target,
        String rewardClassUnlock,
        int rewardPlayerSkillPoints,
        int rewardClassSkillPoints
) {
}
