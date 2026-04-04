package com.extremecraft.ecosystem.core.progression;

public final class ProgressionRuntimeFlags {
    private static volatile boolean debugProgressionBypassEnabled;

    private ProgressionRuntimeFlags() {
    }

    public static boolean isDebugProgressionBypassEnabled() {
        return debugProgressionBypassEnabled;
    }

    public static void setDebugProgressionBypassEnabled(boolean enabled) {
        debugProgressionBypassEnabled = enabled;
    }
}