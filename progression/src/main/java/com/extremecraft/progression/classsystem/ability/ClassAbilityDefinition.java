package com.extremecraft.progression.classsystem.ability;

import java.util.Map;

/**
 * Data model for class abilities loaded from datapacks.
 */
public record ClassAbilityDefinition(
        String id,
        String classId,
        String trigger,
        int cooldownTicks,
        int manaCost,
        Map<String, Double> scaling
) {
}
