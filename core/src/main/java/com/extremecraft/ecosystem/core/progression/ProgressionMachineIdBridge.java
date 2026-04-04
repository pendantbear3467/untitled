package com.extremecraft.ecosystem.core.progression;

import java.util.Locale;
import java.util.function.Function;

public final class ProgressionMachineIdBridge {
    private static volatile Function<String, String> normalizer = ProgressionMachineIdBridge::defaultNormalize;

    private ProgressionMachineIdBridge() {
    }

    public static void setNormalizer(Function<String, String> machineIdNormalizer) {
        normalizer = machineIdNormalizer == null ? ProgressionMachineIdBridge::defaultNormalize : machineIdNormalizer;
    }

    public static String normalizeMachineId(String machineId) {
        return normalizer.apply(machineId);
    }

    private static String defaultNormalize(String machineId) {
        return machineId == null ? "" : machineId.trim().toLowerCase(Locale.ROOT);
    }
}