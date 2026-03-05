package com.extremecraft.skills;

public record SkillDefinition(
        String id,
        int maxLevel,
        double bonusPerLevel
) {
}
