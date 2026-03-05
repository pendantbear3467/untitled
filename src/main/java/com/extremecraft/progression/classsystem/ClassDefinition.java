package com.extremecraft.progression.classsystem;

import java.util.List;
import java.util.Map;

/**
 * Data-driven class metadata with passive and active ability references.
 */
public record ClassDefinition(
        String id,
        String displayName,
        Map<String, Double> passiveBonuses,
        List<String> activeAbilities
) {
}
