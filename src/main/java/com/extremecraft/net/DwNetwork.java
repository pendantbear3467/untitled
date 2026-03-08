package com.extremecraft.net;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Compatibility facade for legacy call sites that still reference dual-wield network helpers.
 * Canonical packet registration and channel ownership live in {@link ModNetwork}.
 */
public final class DwNetwork {
    private DwNetwork() {
    }

    /**
     * Legacy alias retained for compatibility. The canonical protocol id lives in {@link ECConstants#NETWORK_PROTOCOL}.
     */
    @Deprecated(forRemoval = false)
    public static final String PROTOCOL = ECConstants.NETWORK_PROTOCOL;

    /**
     * Legacy alias retained for compatibility. Do not register packets through this field.
     */
    @Deprecated(forRemoval = false)
    public static final SimpleChannel CH = ModNetwork.CHANNEL;

    public static void init() {
        // Compatibility no-op facade: canonical packet registration happens in ModNetwork.
        if (!ModNetwork.isInitialized()) {
            ModNetwork.init();
        }
    }

    public static void sendToServer(Object msg) {
        ModNetwork.CHANNEL.sendToServer(msg);
    }
}
