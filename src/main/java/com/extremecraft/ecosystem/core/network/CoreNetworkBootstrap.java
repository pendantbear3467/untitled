package com.extremecraft.ecosystem.core.network;

import com.extremecraft.network.ModNetwork;

/**
 * Shared network bootstrap wrapper for phased extraction into ExtremeCraft Core.
 */
public final class CoreNetworkBootstrap {
    private CoreNetworkBootstrap() {
    }

    public static void init() {
        ModNetwork.init();
    }

    public static boolean isInitialized() {
        return ModNetwork.isInitialized();
    }
}
