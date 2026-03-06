package com.extremecraft.classsystem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ClassPassives {
    private ClassPassives() {
    }

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
