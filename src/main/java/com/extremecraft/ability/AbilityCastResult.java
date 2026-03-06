package com.extremecraft.ability;

public record AbilityCastResult(String abilityId, Status status, String message, int cooldownTicksRemaining) {
    public enum Status {
        SUCCESS,
        INVALID_REQUEST,
        UNKNOWN_ABILITY,
        NOT_UNLOCKED,
        ON_COOLDOWN,
        INSUFFICIENT_MANA,
        INVALID_TARGET,
        EXECUTION_FAILED,
        ERROR
    }

    public static AbilityCastResult success(String abilityId) {
        return new AbilityCastResult(abilityId, Status.SUCCESS, "ok", 0);
    }

    public static AbilityCastResult failure(String abilityId, Status status, String message) {
        return new AbilityCastResult(abilityId, status, message, 0);
    }

    public static AbilityCastResult cooldown(String abilityId, int ticksRemaining) {
        return new AbilityCastResult(abilityId, Status.ON_COOLDOWN, "cooldown_active", Math.max(0, ticksRemaining));
    }

    public boolean succeeded() {
        return status == Status.SUCCESS;
    }
}

