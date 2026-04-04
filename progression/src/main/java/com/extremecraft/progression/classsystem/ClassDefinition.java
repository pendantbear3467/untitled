package com.extremecraft.progression.classsystem;

import java.util.List;
import java.util.Map;

/**
 * Canonical data-driven class metadata loaded from {@code data/extremecraft/classes}.
 */
public record ClassDefinition(
        String id,
        String displayName,
        Map<String, Double> statScaling,
        double manaRegenModifier,
        Map<String, Double> combatModifiers,
        Map<String, Double> passiveBonuses,
        List<String> activeAbilities,
        List<String> spellAccess,
        int requiredLevel
) {
}
