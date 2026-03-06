package com.extremecraft.platform;

import com.extremecraft.api.ExtremeCraftApiVersions;
import com.extremecraft.api.module.ExtremeCraftModule;

public final class CompatibilityGate {
    private CompatibilityGate() {
    }

    public static boolean isCompatible(ExtremeCraftModule module) {
        return module.apiVersion() == ExtremeCraftApiVersions.EXTREMECRAFT_API_VERSION
                && module.protocolVersion() == ExtremeCraftApiVersions.EXTREMECRAFT_PROTOCOL_VERSION;
    }
}
