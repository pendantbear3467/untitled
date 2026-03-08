package com.extremecraft.dev.validation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ECDataReloadSafety {
    private static final Map<String, Integer> DOMAIN_ENTRY_COUNTS = new LinkedHashMap<>();
    private static long lastUpdateEpochMs;

    private ECDataReloadSafety() {
    }

    public static synchronized void recordDomain(String domain, int entryCount) {
        if (domain == null || domain.isBlank()) {
            return;
        }

        DOMAIN_ENTRY_COUNTS.put(domain, Math.max(0, entryCount));
        lastUpdateEpochMs = System.currentTimeMillis();
    }

    public static synchronized String summary() {
        if (DOMAIN_ENTRY_COUNTS.isEmpty()) {
            return "reload_safety: pending";
        }

        String domains = DOMAIN_ENTRY_COUNTS.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
        return "reload_safety: " + domains + " @" + lastUpdateEpochMs;
    }
}
