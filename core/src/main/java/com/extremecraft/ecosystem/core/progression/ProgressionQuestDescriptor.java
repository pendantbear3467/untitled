package com.extremecraft.ecosystem.core.progression;

public record ProgressionQuestDescriptor(
        String id,
        String title,
        String typeName,
        int target,
        int rewardXp,
        int rewardPlayerSkillPoints,
        int rewardClassSkillPoints,
        String rewardUnlockClass,
        String rewardUnlockStage
) {
}
