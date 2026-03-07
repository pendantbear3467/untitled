package com.extremecraft.net;

import com.extremecraft.network.ModNetwork;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Compatibility facade for legacy call sites that still reference dual-wield network helpers.
 * Canonical packet registration now lives in {@link ModNetwork}.
 */
public final class DwNetwork {
    private DwNetwork() {
    }

    public static final String PROTOCOL = "compat-main";
    public static SimpleChannel CH = ModNetwork.CHANNEL;

    public static void init() {
        CH = ModNetwork.CHANNEL;
    }

    public static void sendToServer(Object msg) {
        ModNetwork.CHANNEL.sendToServer(msg);
    }
}

