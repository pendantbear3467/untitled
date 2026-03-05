package com.extremecraft.progression.stage;

import java.util.Locale;
import java.util.Optional;

public enum ProgressionStage {
    PRIMITIVE,
    INDUSTRIAL,
    AUTOMATION,
    ENERGY,
    ADVANCED,
    ENDGAME;

    public boolean includes(ProgressionStage required) {
        return this.ordinal() >= required.ordinal();
    }

    public static Optional<ProgressionStage> byName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(ProgressionStage.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
