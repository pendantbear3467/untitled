package com.extremecraft.classsystem;

import com.extremecraft.progression.classsystem.ClassIdResolver;
import com.extremecraft.progression.classsystem.ClassDefinition;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges runtime class consumers to the canonical progression class definitions, with a legacy
 * fallback for compatibility.
 */
public final class ClassAccessResolver {
    private ClassAccessResolver() {
    }

    public static PlayerClass resolve(String classId) {
        String key = normalize(classId);
        if (key.isBlank()) {
            key = "warrior";
        }

        ClassDefinition canonical = ClassDefinitions.get(key);
        if (canonical != null) {
            return new PlayerClass(
                    key,
                    canonical.statScaling() == null ? Map.of() : canonical.statScaling(),
                    canonical.manaRegenModifier(),
                    canonical.combatModifiers() == null ? Map.of() : canonical.combatModifiers(),
                    canonical.passiveBonuses() == null ? Map.of() : canonical.passiveBonuses(),
                    normalizeIds(canonical.activeAbilities()),
                    normalizeIds(canonical.spellAccess()),
                    Math.max(1, canonical.requiredLevel())
            );
        }

        // Compatibility fallback only: canonical class-definition loading should satisfy live reads.
        return ClassRegistry.get(key);
    }

    public static List<String> abilityAccess(String classId) {
        PlayerClass playerClass = resolve(classId);
        return playerClass == null ? List.of() : playerClass.abilityAccess();
    }

    public static List<String> spellAccess(String classId) {
        PlayerClass playerClass = resolve(classId);
        return playerClass == null ? List.of() : playerClass.spellAccess();
    }

    private static List<String> normalizeIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(ClassAccessResolver::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private static String normalize(String raw) {
        return ClassIdResolver.normalizeCanonical(raw);
    }
}
