package com.extremecraft.ability;

import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.mana.ManaService;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AbilityEngine {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<UUID, ActiveChannel> ACTIVE_CHANNELS = new LinkedHashMap<>();
    private static final Map<String, Integer> BUILTIN_MANA_COSTS = Map.of(
            "fireball", 18,
            "dash", 8,
            "shockwave", 20,
            "heal", 14
    );

    private static boolean initialized;

    private AbilityEngine() {
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        AbilityRegistry.register("fireball", new FireballAbility());
        AbilityRegistry.register("dash", new DashAbility());
        AbilityRegistry.register("shockwave", new ShockwaveAbility());
        AbilityRegistry.register("heal", new HealAbility());
        initialized = true;
    }

    public static Collection<AbilityDefinition> loadDefinitions() {
        return AbilityRegistry.all();
    }

    public static AbilityCastResult cast(ServerPlayer player, String requestedAbilityId) {
        initialize();
        if (player == null) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.INVALID_REQUEST, "player_missing");
        }

        String abilityId = normalize(requestedAbilityId);
        if (abilityId.isBlank()) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.INVALID_REQUEST, "ability_id_missing");
        }

        Ability ability = AbilityRegistry.runtime(abilityId);
        AbilityDefinition definition = AbilityRegistry.get(abilityId);
        if (ability == null && definition == null) {
            return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.UNKNOWN_ABILITY, "ability_not_registered");
        }

        if (!validateRequirements(player, abilityId, ability, definition)) {
            return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.NOT_UNLOCKED, "requirements_failed");
        }

        int remaining = AbilityCooldownManager.remainingTicks(player, abilityId);
        if (remaining > 0) {
            AbilityCooldownManager.sync(player);
            return AbilityCastResult.cooldown(abilityId, remaining);
        }

        int manaCost = resolveManaCost(abilityId, definition);
        if (manaCost > 0 && !ManaService.tryConsume(player, manaCost)) {
            return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.INSUFFICIENT_MANA, "insufficient_mana");
        }

        AbilityContext context = AbilityContext.of(player, definition).withManaCost(manaCost);

        boolean executed;
        try {
            if (ability != null) {
                ability.execute(context);
                executed = true;
            } else {
                executed = AbilityExecutor.executeDefinition(context);
            }
        } catch (Exception ex) {
            LOGGER.error("[AbilityEngine] Failed to execute ability {}", abilityId, ex);
            return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.ERROR, "execution_error");
        }

        if (!executed) {
            return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.EXECUTION_FAILED, "execution_failed");
        }

        int cooldownTicks = resolveCooldown(ability, definition);
        AbilityCooldownManager.startCooldown(player, abilityId, cooldownTicks);
        AbilityCooldownManager.sync(player);
        return AbilityCastResult.success(abilityId);
    }

    public static void beginChannel(AbilityContext context, int channelTicks, int pulseIntervalTicks, double radius, AbilityEffect pulseEffect) {
        if (context == null || context.player() == null || pulseEffect == null) {
            return;
        }

        String abilityId = context.definition() == null ? "runtime_channel" : context.definition().id();
        long now = context.world().getGameTime();
        ActiveChannel channel = new ActiveChannel(
                abilityId,
                Math.max(1, context.abilityLevel()),
                Math.max(0, context.manaCost()),
                now + Math.max(1, channelTicks),
                now + 1,
                Math.max(1, pulseIntervalTicks),
                Math.max(1.0D, radius),
                pulseEffect
        );

        ACTIVE_CHANNELS.put(context.player().getUUID(), channel);
    }

    public static void tickChanneling(ServerPlayer player) {
        ActiveChannel active = ACTIVE_CHANNELS.get(player.getUUID());
        if (active == null) {
            return;
        }

        long now = player.level().getGameTime();
        if (now >= active.endTick) {
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        if (now < active.nextPulseTick) {
            return;
        }

        AbilityDefinition definition = AbilityRegistry.get(active.abilityId);
        AbilityContext pulseContext = AbilityContext.of(player, definition)
                .withAbilityLevel(active.abilityLevel)
                .withManaCost(active.manaCost);

        try {
            switch (active.pulseEffect.type()) {
                case "projectile" -> AbilityExecutor.executeProjectile(pulseContext, active.pulseEffect);
                case "move", "movement", "dash" -> AbilityExecutor.executeMovement(pulseContext, active.pulseEffect.value());
                default -> AbilityExecutor.executeAreaEffect(pulseContext, active.radius, List.of(active.pulseEffect));
            }
        } catch (Exception ex) {
            LOGGER.error("[AbilityEngine] Failed to tick channeled ability {}", active.abilityId, ex);
            ACTIVE_CHANNELS.remove(player.getUUID());
            return;
        }

        active.nextPulseTick = now + active.pulseIntervalTicks;
    }

    private static boolean validateRequirements(ServerPlayer player, String abilityId, Ability ability, AbilityDefinition definition) {
        if (definition == null) {
            return true;
        }

        // Existing class gating remains for definition-only abilities.
        if (ability == null || !definition.requiredClass().isBlank()) {
            return ClassAbilityBindings.canUseAbility(player, abilityId, definition.requiredClass());
        }

        return true;
    }

    private static int resolveManaCost(String abilityId, AbilityDefinition definition) {
        if (definition != null && definition.manaCost() > 0) {
            return definition.manaCost();
        }
        return BUILTIN_MANA_COSTS.getOrDefault(abilityId, 0);
    }

    private static int resolveCooldown(Ability ability, AbilityDefinition definition) {
        if (definition != null && definition.cooldownTicks() > 0) {
            return definition.cooldownTicks();
        }
        return ability == null ? 0 : Math.max(0, ability.getCooldown());
    }

    private static String normalize(String abilityId) {
        return abilityId == null ? "" : abilityId.trim().toLowerCase();
    }

    private static final class ActiveChannel {
        private final String abilityId;
        private final int abilityLevel;
        private final int manaCost;
        private final long endTick;
        private long nextPulseTick;
        private final int pulseIntervalTicks;
        private final double radius;
        private final AbilityEffect pulseEffect;

        private ActiveChannel(String abilityId,
                              int abilityLevel,
                              int manaCost,
                              long endTick,
                              long nextPulseTick,
                              int pulseIntervalTicks,
                              double radius,
                              AbilityEffect pulseEffect) {
            this.abilityId = abilityId;
            this.abilityLevel = abilityLevel;
            this.manaCost = manaCost;
            this.endTick = endTick;
            this.nextPulseTick = nextPulseTick;
            this.pulseIntervalTicks = pulseIntervalTicks;
            this.radius = radius;
            this.pulseEffect = pulseEffect;
        }
    }
}


