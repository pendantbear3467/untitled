package com.extremecraft.ability;

import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.magic.mana.ManaService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityEngine {
    /**
     * Runtime executor for abilities from both built-in registrations and datapack definitions.
     * <p>
     * Ability metadata is sourced through {@link AbilityRegistry}, progression gating is delegated to
     * class bindings, and mana/cooldowns are synchronized through the progression and mana services.
     */
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int CAST_MIN_INTERVAL_TICKS = 1;
    private static final double MAX_NETWORK_TARGET_DISTANCE = 64.0D;

    private static final Map<UUID, ActiveChannel> ACTIVE_CHANNELS = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> LAST_CAST_TICK = new ConcurrentHashMap<>();
    private static final Set<UUID> CAST_IN_PROGRESS = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> BUILTIN_MANA_COSTS = Map.of(
            "fireball", 18,
            "dash", 8,
            "shockwave", 20,
            "heal", 14,
            "firebolt", 12,
            "blink", 10,
            "arcane_shield", 24,
            "meteor", 40
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
        return cast(player, requestedAbilityId, player == null ? null : player.getUUID(), null);
    }

    public static AbilityCastResult cast(ServerPlayer player, String requestedAbilityId, UUID requestPlayerUuid, Vec3 requestedTargetPosition) {
        initialize();
        if (player == null || player.isSpectator()) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.EXECUTION_FAILED, "player_missing");
        }

        UUID playerId = player.getUUID();
        if (requestPlayerUuid != null && !playerId.equals(requestPlayerUuid)) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.EXECUTION_FAILED, "uuid_mismatch");
        }

        long now = player.level().getGameTime();
        if (isCastRateLimited(playerId, now)) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.EXECUTION_FAILED, "rate_limited");
        }

        if (!CAST_IN_PROGRESS.add(playerId)) {
            return AbilityCastResult.failure("", AbilityCastResult.Status.EXECUTION_FAILED, "recursive_cast");
        }

        LAST_CAST_TICK.put(playerId, now);
        try {
            String abilityId = normalize(requestedAbilityId);
            if (abilityId.isBlank()) {
                return AbilityCastResult.failure("", AbilityCastResult.Status.EXECUTION_FAILED, "ability_id_missing");
            }

            Ability ability = AbilityRegistry.runtime(abilityId);
            AbilityDefinition definition = AbilityRegistry.get(abilityId);
            if (ability == null && definition == null) {
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.UNKNOWN_ABILITY, "ability_not_registered");
            }

            if (!validateRequirements(player, abilityId, ability, definition)) {
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.EXECUTION_FAILED, "requirements_failed");
            }

            int remaining = AbilityCooldownManager.remainingTicks(player, abilityId);
            if (remaining > 0) {
                AbilityCooldownManager.sync(player);
                return AbilityCastResult.cooldown(abilityId, remaining);
            }

            int manaCost = resolveManaCost(abilityId, definition);
            if (manaCost > 0 && !hasEnoughMana(player, manaCost)) {
                ManaService.sync(player);
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.INSUFFICIENT_MANA, "insufficient_mana");
            }

            AbilityContext baseContext = AbilityContext.of(player, definition).withManaCost(manaCost);
            if (requestedTargetPosition != null) {
                if (!isFinite(requestedTargetPosition)) {
                    return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.INVALID_TARGET, "invalid_target");
                }

                if (player.position().distanceToSqr(requestedTargetPosition) > (MAX_NETWORK_TARGET_DISTANCE * MAX_NETWORK_TARGET_DISTANCE)) {
                    return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.INVALID_TARGET, "target_out_of_range");
                }

                baseContext = baseContext.withTarget(null, requestedTargetPosition);
            }

            AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(baseContext);
            if (definition != null && !isTargetValid(definition.targetType(), targets)) {
                return AbilityCastResult.invalidTarget(abilityId, "invalid_target");
            }

            AbilityContext resolvedContext = baseContext.withTarget(
                    targets.entities().isEmpty() ? null : targets.entities().get(0),
                    targets.center()
            );

            if (manaCost > 0 && !ManaService.tryConsume(player, manaCost)) {
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.INSUFFICIENT_MANA, "insufficient_mana");
            }

            boolean executed;
            try {
                if (ability != null) {
                    ability.execute(resolvedContext);
                    executed = true;
                } else {
                    executed = AbilityExecutor.executeDefinition(resolvedContext);
                }
            } catch (Exception ex) {
                LOGGER.error("[AbilityEngine] Failed to execute ability {}", abilityId, ex);
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.EXECUTION_FAILED, "execution_error");
            }

            if (!executed) {
                return AbilityCastResult.failure(abilityId, AbilityCastResult.Status.EXECUTION_FAILED, "execution_failed");
            }

            int cooldownTicks = resolveCooldown(player, ability, definition);
            AbilityCooldownManager.startCooldown(player, abilityId, cooldownTicks);
            AbilityCooldownManager.sync(player);
            return AbilityCastResult.success(abilityId);
        } finally {
            CAST_IN_PROGRESS.remove(playerId);
        }
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

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            clearPlayer(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }

        ACTIVE_CHANNELS.remove(playerId);
        LAST_CAST_TICK.remove(playerId);
        CAST_IN_PROGRESS.remove(playerId);
    }

    private static boolean isCastRateLimited(UUID playerId, long now) {
        long lastCast = LAST_CAST_TICK.getOrDefault(playerId, Long.MIN_VALUE / 2L);
        return (now - lastCast) < CAST_MIN_INTERVAL_TICKS;
    }

    private static boolean validateRequirements(ServerPlayer player, String abilityId, Ability ability, AbilityDefinition definition) {
        if (definition == null) {
            return true;
        }

        if (hasDevGrantedAbility(player, abilityId)) {
            return true;
        }

        if (ability == null || !definition.requiredClass().isBlank()) {
            return ClassAbilityBindings.canUseAbility(player, abilityId, definition.requiredClass());
        }

        return true;
    }

    private static boolean hasEnoughMana(ServerPlayer player, int manaCost) {
        return ManaApi.get(player).map(mana -> mana.currentMana() >= manaCost).orElse(false);
    }

    private static int resolveManaCost(String abilityId, AbilityDefinition definition) {
        if (definition != null && definition.manaCost() > 0) {
            return definition.manaCost();
        }
        return BUILTIN_MANA_COSTS.getOrDefault(abilityId, 0);
    }

    private static int resolveCooldown(ServerPlayer player, Ability ability, AbilityDefinition definition) {
        int base = 0;
        if (definition != null && definition.cooldownTicks() > 0) {
            base = definition.cooldownTicks();
        } else if (ability != null) {
            base = Math.max(0, ability.getCooldown());
        }

        float reduction = com.extremecraft.progression.capability.PlayerStatsApi.get(player)
                .map(com.extremecraft.progression.capability.PlayerStatsCapability::cooldownReduction)
                .orElse(0.0F);

        reduction = Math.max(0.0F, Math.min(0.8F, reduction));
        return Math.max(0, Math.round(base * (1.0F - reduction)));
    }

    private static boolean isTargetValid(AbilityDefinition.TargetType targetType, AbilityTargetResolver.TargetBundle bundle) {
        if (targetType == null) {
            return true;
        }

        return switch (targetType) {
            case ENTITY -> !bundle.entities().isEmpty();
            case SELF, AREA, PROJECTILE, NONE -> true;
        };
    }

    private static boolean hasDevGrantedAbility(ServerPlayer player, String abilityId) {
        if (abilityId == null || abilityId.isBlank()) {
            return false;
        }

        net.minecraft.nbt.CompoundTag dev = player.getPersistentData().getCompound("ec_dev");
        net.minecraft.nbt.ListTag granted = dev.getList("abilities", net.minecraft.nbt.Tag.TAG_STRING);
        for (net.minecraft.nbt.Tag entry : granted) {
            if (abilityId.equalsIgnoreCase(entry.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFinite(Vec3 target) {
        return Double.isFinite(target.x) && Double.isFinite(target.y) && Double.isFinite(target.z);
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
