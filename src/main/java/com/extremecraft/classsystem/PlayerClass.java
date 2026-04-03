package com.extremecraft.classsystem;

import java.util.List;
import java.util.Map;

/**
 * Immutable runtime view of a playable class definition.
 *
 * <p>Instances are produced by class data loaders/resolvers and consumed by ability, spell,
 * progression, and passive systems.</p>
 */
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
    /**
     * Returns per-stat scaling multiplier, defaulting to neutral 1.0 when unspecified.
     */
    public double statScale(String statId) {
        return statScaling.getOrDefault(statId, 1.0D);
    }
}
