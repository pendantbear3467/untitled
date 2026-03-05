package com.extremecraft.progression.unlock;

public record UnlockRule(
        String unlock,
        String requiredClass,
        String requiredSkill,
        int requiredSkillLevel,
        String requiredQuest,
        String requiredStage
) {
}
