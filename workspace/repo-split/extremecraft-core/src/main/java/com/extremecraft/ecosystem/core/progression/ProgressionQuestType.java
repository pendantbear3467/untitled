package com.extremecraft.ecosystem.core.progression;

import java.util.Locale;
import java.util.Optional;

public enum ProgressionQuestType {
    KILL,
    COLLECTION,
    EXPLORATION,
    CRAFTING,
    BOSS;

    public static Optional<ProgressionQuestType> fromName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ProgressionQuestType.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}