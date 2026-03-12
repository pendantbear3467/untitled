package com.extremecraft.classsystem;

import com.extremecraft.progression.classsystem.ClassIdResolver;
import com.extremecraft.progression.classsystem.ClassDefinition;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridges legacy class runtime consumers to the canonical progression class definitions.
 */
public final class ClassAccessResolver {
    private ClassAccessResolver() {
    }

    public static PlayerClass resolve(String classId) {
        String key = normalize(classId);
        if (key.isBlank()) {
            key = "warrior";
        }

        PlayerClass fromLegacyRegistry = ClassRegistry.get(key);
        if (fromLegacyRegistry != null) {
            return fromLegacyRegistry;
        }

        ClassDefinition canonical = ClassDefinitions.get(key);
        if (canonical == null) {
            return null;
        }

        Map<String, Double> passives = canonical.passiveBonuses() == null ? Map.of() : canonical.passiveBonuses();
        double manaRegenModifier = passives.getOrDefault("mana_regen_modifier", passives.getOrDefault("mana_regen", 0.0D));
        List<String> activeAbilities = normalizeIds(canonical.activeAbilities());

        return new PlayerClass(
                key,
                Map.of(),
                manaRegenModifier,
                Map.of(),
                passives,
                activeAbilities,
                List.of(),
                1
        );
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
