package com.extremecraft.reactor;

import java.util.Locale;

/**
 * First-release reactor identity helper.
 *
 * <p>The live registry id remains {@code fusion_reactor} for compatibility, but
 * first-release gameplay and UI treat it as the canonical fission reactor line.</p>
 */
public final class ReactorIdentity {
    public static final String CANONICAL_RUNTIME_ID = "fusion_reactor";
    public static final String COMPAT_ALIAS_ID = "fission_reactor";

    private ReactorIdentity() {
    }

    public static String normalizeMachineId(String machineId) {
        if (machineId == null) {
            return "";
        }

        String normalized = machineId.trim().toLowerCase(Locale.ROOT);
        if (COMPAT_ALIAS_ID.equals(normalized)) {
            return CANONICAL_RUNTIME_ID;
        }
        return normalized;
    }

    public static boolean isFirstReleaseReactor(String machineId) {
        return CANONICAL_RUNTIME_ID.equals(normalizeMachineId(machineId));
    }
}
