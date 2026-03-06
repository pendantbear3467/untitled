package com.extremecraft.client.gui.debug;

public final class DeveloperOverlayState {
    private static boolean enabled;

    private DeveloperOverlayState() {
    }

    public static synchronized boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public static synchronized boolean isEnabled() {
        return enabled;
    }
}
