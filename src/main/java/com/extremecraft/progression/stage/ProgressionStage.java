package com.extremecraft.progression.stage;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public enum ProgressionStage {
    PRIMITIVE(0, true),
    ENERGY(1, true),
    INDUSTRIAL(2, true),
    ADVANCED(3, true),
    ENDGAME(4, true),
    @Deprecated(forRemoval = false)
    AUTOMATION(2, false);

    private static final Map<String, ProgressionStage> LEGACY_ALIASES = Map.of(
            "automation", INDUSTRIAL
    );

    private final int rank;
    private final boolean canonicalRuntimeStage;

    ProgressionStage(int rank, boolean canonicalRuntimeStage) {
        this.rank = rank;
        this.canonicalRuntimeStage = canonicalRuntimeStage;
    }

    public boolean includes(ProgressionStage required) {
        if (required == null) {
            return true;
        }
        return rank >= required.rank;
    }

    public boolean canonicalRuntimeStage() {
        return canonicalRuntimeStage;
    }

    public static Optional<ProgressionStage> byName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        ProgressionStage alias = LEGACY_ALIASES.get(normalized);
        if (alias != null) {
            return Optional.of(alias);
        }

        try {
            ProgressionStage parsed = ProgressionStage.valueOf(normalized.toUpperCase(Locale.ROOT));
            return Optional.of(parsed == AUTOMATION ? INDUSTRIAL : parsed);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
