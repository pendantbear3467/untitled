package com.extremecraft.quest;

public record QuestDefinition(
        String id,
        String title,
        QuestType type,
        int target,
        int rewardXp,
        int rewardPlayerSkillPoints,
        int rewardClassSkillPoints,
        String rewardUnlockClass,
        String rewardUnlockStage
) {
}
