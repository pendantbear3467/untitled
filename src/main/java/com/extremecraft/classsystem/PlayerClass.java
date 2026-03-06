package com.extremecraft.classsystem;

import java.util.List;
import java.util.Map;

public record PlayerClass(
        String id,
        Map<String, Double> statScaling,
        double manaRegenModifier,
        Map<String, Double> combatModifiers,
        Map<String, Double> passives,
        List<String> abilityAccess,
        List<String> spellAccess,
        int requiredLevel
) {
    public double statScale(String statId) {
        return statScaling.getOrDefault(statId, 1.0D);
    }
}
