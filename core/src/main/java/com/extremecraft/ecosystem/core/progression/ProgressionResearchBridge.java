package com.extremecraft.ecosystem.core.progression;

import java.util.function.BiFunction;

public final class ProgressionResearchBridge {
    private static volatile BiFunction<Object, String, Boolean> hasResearchProvider = (carrier, id) -> false;

    private ProgressionResearchBridge() {
    }

    public static void setProvider(BiFunction<Object, String, Boolean> provider) {
        hasResearchProvider = provider == null ? (carrier, id) -> false : provider;
    }

    public static boolean hasResearch(Object carrier, String researchId) {
        return Boolean.TRUE.equals(hasResearchProvider.apply(carrier, researchId));
    }
}