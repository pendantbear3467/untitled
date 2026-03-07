package com.extremecraft.net;

import com.extremecraft.network.ModNetwork;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Compatibility facade for legacy call sites that still reference dual-wield network helpers.
 * Canonical packet registration and channel ownership live in {@link ModNetwork}.
 */
public final class DwNetwork {
    private DwNetwork() {
    }

    public static final String PROTOCOL = "compat-main";

    /**
     * Legacy alias retained for compatibility. Do not register packets through this field.
     */
    @Deprecated(forRemoval = false)
    public static final SimpleChannel CH = ModNetwork.CHANNEL;

    public static void init() {
        if (!ModNetwork.isInitialized()) {
            ModNetwork.init();
        }
    }

    public static void sendToServer(Object msg) {
        ModNetwork.CHANNEL.sendToServer(msg);
    }
}
