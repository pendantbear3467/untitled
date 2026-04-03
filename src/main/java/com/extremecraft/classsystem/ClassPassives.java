package com.extremecraft.classsystem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregates class passive and combat modifier values into one passive map.
 */
public final class ClassPassives {
    private ClassPassives() {
    }

    /**
     * Merges base passives, explicit mana regen modifier, and combat modifiers.
     *
     * <p>Returned map is immutable to prevent accidental mutation by downstream systems.</p>
     */
    public static Map<String, Double> resolve(PlayerClass playerClass) {
        if (playerClass == null) {
            return Map.of();
        }

        Map<String, Double> merged = new LinkedHashMap<>();
        merged.putAll(playerClass.passives());
        merged.put("mana_regen", playerClass.manaRegenModifier());
        for (Map.Entry<String, Double> entry : playerClass.combatModifiers().entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
        return Map.copyOf(merged);
    }
}
