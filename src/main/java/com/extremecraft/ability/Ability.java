package com.extremecraft.ability;

/**
 * Minimal execution contract for hard-coded (runtime) abilities.
 *
 * <p>Data-driven abilities are represented by {@link AbilityDefinition} and executed by
 * {@link AbilityExecutor}; this interface covers Java-authored abilities that need direct code.
 * Both models are unified by {@link AbilityEngine}.</p>
 */
public interface Ability {
    /**
     * Stable normalized id used by command input, cooldown keys, unlock checks, and network sync.
     */
    String getId();

    /**
     * Base cooldown in ticks before reductions from progression stats are applied.
     */
    int getCooldown();

    /**
     * Executes ability behavior on server side using a fully resolved runtime context.
     */
    void execute(AbilityContext context);
}
