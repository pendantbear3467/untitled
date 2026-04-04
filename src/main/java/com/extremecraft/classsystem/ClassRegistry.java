package com.extremecraft.classsystem;

import com.extremecraft.progression.classsystem.ClassDefinition;
import com.extremecraft.progression.classsystem.ClassIdResolver;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * COMPATIBILITY ADAPTER: legacy-facing class registry projection.
 *
 * <p>Canonical gameplay class definitions now load once through
 * {@code progression.classsystem.data.ClassDefinitionLoader}. This adapter projects those
 * canonical definitions into the older {@link PlayerClass} shape for callers that have not been
 * migrated yet.</p>
 */
public final class ClassRegistry {
    private ClassRegistry() {
    }

    public static PlayerClass get(String id) {
        String normalized = normalize(id);
        if (normalized.isBlank()) {
            return null;
        }
        return toPlayerClass(ClassDefinitions.get(normalized));
    }

    public static Collection<PlayerClass> all() {
        return ClassDefinitions.all().stream()
                .map(ClassRegistry::toPlayerClass)
                .collect(Collectors.toUnmodifiableList());
    }

    public static int size() {
        return ClassDefinitions.all().size();
    }

    private static PlayerClass toPlayerClass(ClassDefinition definition) {
        if (definition == null) {
            return null;
        }

        return new PlayerClass(
                definition.id(),
                emptyIfNull(definition.statScaling()),
                definition.manaRegenModifier(),
                emptyIfNull(definition.combatModifiers()),
                emptyIfNull(definition.passiveBonuses()),
                normalizeIds(definition.activeAbilities()),
                normalizeIds(definition.spellAccess()),
                Math.max(1, definition.requiredLevel())
        );
    }

    private static Map<String, Double> emptyIfNull(Map<String, Double> values) {
        return values == null ? Map.of() : values;
    }

    private static List<String> normalizeIds(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(ClassRegistry::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toUnmodifiableList());
    }

    private static String normalize(String raw) {
        return ClassIdResolver.normalizeCanonical(raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT));
    }
}
