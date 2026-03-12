package com.extremecraft.progression.classsystem;

import java.util.Locale;
import java.util.Map;

/**
 * Normalizes legacy class ids onto the canonical first-release class taxonomy.
 */
public final class ClassIdResolver {
    public static final String DEFAULT_CLASS_ID = "warrior";

    private static final Map<String, String> LEGACY_ALIASES = Map.ofEntries(
            Map.entry("fighter", "warrior"),
            Map.entry("miner", "engineer"),
            Map.entry("explorer", "ranger"),
            Map.entry("scientist", "technomancer"),
            Map.entry("medic", "warden"),
            Map.entry("medical", "warden"),
            Map.entry("trader", "chronomancer")
    );

    private ClassIdResolver() {
    }

    public static String normalizeCanonical(String classId) {
        if (classId == null) {
            return "";
        }

        String normalized = classId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "";
        }

        return LEGACY_ALIASES.getOrDefault(normalized, normalized);
    }

    public static String defaultIfBlank(String classId) {
        String normalized = normalizeCanonical(classId);
        return normalized.isBlank() ? DEFAULT_CLASS_ID : normalized;
    }

    public static boolean matches(String left, String right) {
        String normalizedLeft = normalizeCanonical(left);
        String normalizedRight = normalizeCanonical(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return false;
        }
        return normalizedLeft.equals(normalizedRight);
    }
}
