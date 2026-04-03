package com.extremecraft.ability;

/**
 * Canonical cast response payload returned by {@link AbilityEngine}.
 *
 * <p>The response is intentionally compact and network-friendly: status conveys machine-readable
 * outcome while message carries a short diagnostic token for UI/telemetry mapping.</p>
 */
public record AbilityCastResult(String abilityId, Status status, String message, int cooldownTicksRemaining) {
    /**
     * Ordered from "happy path" to common failure categories so call sites can switch over status
     * without inspecting message strings.
     */
    public enum Status {
        SUCCESS,
        UNKNOWN_ABILITY,
        ON_COOLDOWN,
        INSUFFICIENT_MANA,
        INVALID_TARGET,
        EXECUTION_FAILED
    }

    /**
     * Success helper with no remaining cooldown at response time.
     */
    public static AbilityCastResult success(String abilityId) {
        return new AbilityCastResult(abilityId, Status.SUCCESS, "ok", 0);
    }

    /**
     * Generic failure helper for deterministic non-cooldown failures.
     */
    public static AbilityCastResult failure(String abilityId, Status status, String message) {
        return new AbilityCastResult(abilityId, status, message, 0);
    }

    /**
     * Cooldown-specific helper preserving ticks remaining for HUD synchronization.
     */
    public static AbilityCastResult cooldown(String abilityId, int ticksRemaining) {
        return new AbilityCastResult(abilityId, Status.ON_COOLDOWN, "cooldown_active", Math.max(0, ticksRemaining));
    }

    /**
     * Helper used when resolver/validation rejects target selection.
     */
    public static AbilityCastResult invalidTarget(String abilityId, String message) {
        return new AbilityCastResult(abilityId, Status.INVALID_TARGET, message == null ? "invalid_target" : message, 0);
    }

    /**
     * Convenience predicate used by thin wrappers such as command and keybind handlers.
     */
    public boolean succeeded() {
        return status == Status.SUCCESS;
    }
}
