package com.extremecraft;

import com.extremecraft.core.ECConstants;

/**
 * Legacy compatibility shim. Use com.extremecraft.core.ExtremeCraft as the mod entrypoint.
 */
public final class ExtremeCraftMod {
    /**
     * Maintained for legacy references expecting MODID on this type.
     */
    public static final String MODID = ECConstants.MODID;

    /**
     * Utility holder; no instances.
     */
    private ExtremeCraftMod() {}
}
