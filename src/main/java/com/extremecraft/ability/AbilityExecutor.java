package com.extremecraft.ability;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import com.extremecraft.progression.BuffStackingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Executes effect payloads for data-driven abilities.
 *
 * <p>This class is deliberately stateless: all runtime state is supplied through
 * {@link AbilityContext}, and long-lived cast bookkeeping stays in {@link AbilityEngine}.</p>
 */
public final class AbilityExecutor {
    private static final double MAX_TELEPORT_DISTANCE = 64.0D;

    private AbilityExecutor() {
    }

    /**
     * Legacy convenience wrapper that delegates to the full cast pipeline.
     */
    public static boolean tryActivate(ServerPlayer player, String requestedAbilityId) {
        return AbilityEngine.cast(player, requestedAbilityId).succeeded();
    }

    /**
     * LEGACY wrapper kept for older spell-specific callers.
     *
     * <p>The canonical compiled-definition execution path for active abilities, spells,
     * class abilities, and module-triggered effects is {@link #executeDefinition(AbilityContext)}.</p>
     */
    public static boolean executeSpellAbility(ServerPlayer player, AbilityDefinition definition) {
        return executeDefinition(AbilityContext.of(player, definition));
    }

    /**
     * Canonical compiled-definition execution path shared by abilities, spells, class abilities,
     * and module-triggered effects.
     */
    public static boolean executeDefinition(AbilityContext context) {
        if (context == null || context.player() == null || context.definition() == null || context.player().level().isClientSide()) {
            return false;
        }

        if (context.definition().effects().isEmpty()) {
            return false;
        }

        AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(context);
        AbilityContext resolved = context.withTarget(targets.entities().isEmpty() ? null : targets.entities().get(0), targets.center());
        applyEffects(resolved, targets.entities(), targets.center());
        return true;
    }

    /**
     * Executes one projectile effect (usually arrow-based spell projectile).
     */
    public static void executeProjectile(AbilityContext context, AbilityEffect effect) {
        if (context == null || effect == null) {
            return;
        }
        applyProjectile(context, effect);
    }

    /**
     * Overload with default projectile profile used by legacy call sites.
     */
    public static void executeProjectile(AbilityContext context) {
        executeProjectile(context, new AbilityEffect("projectile", 2.0D, 0, 0, "", Map.of()));
    }

    /**
     * Applies provided effects to all entities inside a simple radius around the caster.
     */
    public static void executeAreaEffect(AbilityContext context, double radius, List<AbilityEffect> effects) {
        if (context == null || context.player() == null || context.player().level().isClientSide() || effects == null || effects.isEmpty()) {
            return;
        }

        List<LivingEntity> targets = context.player().level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(context.player().blockPosition()).inflate(Math.max(1.0D, radius)),
                entity -> entity.isAlive() && entity != context.player()
        );

        applyEffects(context, targets, context.player().position(), effects);
    }

    /**
     * Performs a short directional movement/teleport burst.
     */
    public static void executeMovement(AbilityContext context, double distance) {
        if (context == null || context.player() == null || context.player().level().isClientSide()) {
            return;
        }
        if (!(context.player().level() instanceof ServerLevel)) {
            return;
        }

        double travel = Math.max(0.25D, distance);
        Vec3 look = context.player().getLookAngle().normalize();
        double x = context.player().getX() + (look.x * travel);
        double z = context.player().getZ() + (look.z * travel);

        context.player().teleportTo(x, context.player().getY(), z);
        context.player().fallDistance = 0.0F;
        context.player().swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
    }

    /**
     * Applies heal values to explicit targets or defaults to the caster.
     */
    public static void executeHeal(AbilityContext context, List<? extends LivingEntity> targets, AbilityEffect effect) {
        if (context == null || effect == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<LivingEntity> castTargets = (List<LivingEntity>) (targets == null ? List.of(context.player()) : targets);
        applyHeal(castTargets, effect);
    }

    /**
     * Applies a beneficial or harmful mob-effect payload.
     */
    public static void executeBuff(AbilityContext context, List<? extends LivingEntity> targets, AbilityEffect effect, boolean harmful) {
        if (context == null || effect == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        List<LivingEntity> castTargets = (List<LivingEntity>) (targets == null ? List.of(context.player()) : targets);
        applyBuff(castTargets, effect, harmful);
    }

    /**
     * Starts channeling by converting one effect into periodic pulse configuration.
     */
    public static void executeChannel(AbilityContext context, AbilityEffect effect) {
        if (context == null || context.player() == null || effect == null) {
            return;
        }

        int channelTicks = Math.max(20, effect.duration() <= 0 ? 60 : effect.duration() * 20);
        int pulseTicks = Math.max(2, effect.scalars().getOrDefault("pulse_ticks", 10.0D).intValue());
        double radius = effect.scalars().getOrDefault("radius",
                context.definition() == null ? 4.0D : Math.max(1.0D, context.definition().radius()));

        AbilityEffect pulseEffect = new AbilityEffect(
                resolveChannelPulseType(effect),
                effect.value(),
                Math.max(0, effect.duration()),
                Math.max(0, effect.amplifier()),
                effect.id(),
                effect.scalars()
        );

        AbilityEngine.beginChannel(context, channelTicks, pulseTicks, radius, pulseEffect);
    }

    /**
     * Maps channel id values to runtime effect types used by pulse execution.
     */
    private static String resolveChannelPulseType(AbilityEffect effect) {
        String id = effect.id();
        if ("heal".equals(id) || "ignite".equals(id) || "buff".equals(id) || "debuff".equals(id) || "projectile".equals(id)) {
            return id;
        }
        return "damage";
    }

    /**
     * Uses the definition's effect list by default.
     */
    private static void applyEffects(AbilityContext context, List<LivingEntity> targets, Vec3 center) {
        applyEffects(context, targets, center, context.definition() == null ? List.of() : context.definition().effects());
    }

    /**
     * Central effect dispatcher. Each effect type delegates to a focused helper.
     */
    private static void applyEffects(AbilityContext context, List<LivingEntity> targets, Vec3 center, List<AbilityEffect> effects) {
        for (AbilityEffect effect : effects) {
            switch (effect.type()) {
                case "damage" -> applyDamage(context, targets, effect);
                case "ignite" -> applyIgnite(targets, effect);
                case "heal" -> applyHeal(targets.isEmpty() ? List.of(context.caster()) : targets, effect);
                case "buff" -> applyBuff(targets.isEmpty() ? List.of(context.caster()) : targets, effect, false);
                case "debuff" -> applyBuff(targets, effect, true);
                case "summon" -> applySummon(context, center, effect);
                case "projectile" -> applyProjectile(context, effect);
                case "teleport" -> applyTeleport(context, effect);
                case "move", "movement", "dash" -> executeMovement(context, effect.value());
                case "channel" -> executeChannel(context, effect);
                default -> {
                }
            }
        }
    }

    /**
     * Routes ability damage through combat engine so mitigation/crit/status systems stay unified.
     */
    private static void applyDamage(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect) {
        if (context == null || context.caster() == null || context.caster().level().isClientSide() || targets.isEmpty()) {
            return;
        }
        if (!(context.caster().level() instanceof ServerLevel)) {
            return;
        }

        float damage = (float) Math.max(0.0D, effect.value());
        DamageType damageType = switch ((effect.id() == null ? "" : effect.id()).trim().toLowerCase()) {
            case "physical", "weapon", "melee" -> DamageType.PHYSICAL;
            case "fire", "ignite", "burn" -> DamageType.FIRE;
            case "ice", "frost", "freeze" -> DamageType.ICE;
            case "lightning", "shock" -> DamageType.LIGHTNING;
            case "poison", "toxin" -> DamageType.POISON;
            case "void" -> DamageType.VOID;
            case "holy", "radiant" -> DamageType.HOLY;
            case "bleed", "bleeding" -> DamageType.BLEED;
            default -> DamageType.MAGIC;
        };

        for (LivingEntity target : targets) {
            CombatEngine.applyDamage(DamageContext.builder()
                    .attacker(context.caster())
                    .target(target)
                    .damageAmount(damage)
                    .damageType(damageType)
                    .abilitySource(context.definition() == null ? "runtime" : context.definition().id())
                    .weaponSource(context.caster().getMainHandItem())
                    .armorValue(target.getArmorValue())
                    .build());
        }
    }

    /**
     * Applies vanilla fire timer effect.
     */
    private static void applyIgnite(List<LivingEntity> targets, AbilityEffect effect) {
        int seconds = Math.max(1, effect.duration());
        for (LivingEntity target : targets) {
            if (target.level().isClientSide()) {
                continue;
            }
            target.setSecondsOnFire(seconds);
        }
    }

    /**
     * Applies direct heal amount.
     */
    private static void applyHeal(List<LivingEntity> targets, AbilityEffect effect) {
        float amount = (float) Math.max(0.0D, effect.value());
        for (LivingEntity target : targets) {
            if (target.level().isClientSide()) {
                continue;
            }
            target.heal(amount);
        }
    }

    /**
     * Applies mob effects and records stack metadata for progression UI/logic.
     */
    private static void applyBuff(List<LivingEntity> targets, AbilityEffect effect, boolean harmful) {
        ResourceLocation effectId = ResourceLocation.tryParse(effect.id());
        if (effectId == null) {
            return;
        }

        MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (mobEffect == null || mobEffect.isBeneficial() == harmful) {
            return;
        }

        int durationTicks = Math.max(20, effect.duration() * 20);
        for (LivingEntity target : targets) {
            if (target.level().isClientSide()) {
                continue;
            }

            MobEffectInstance instance = new MobEffectInstance(mobEffect, durationTicks, effect.amplifier());
            target.addEffect(instance);
            BuffStackingSystem.track(target, effect.type() + ":" + effect.id(), durationTicks, effect.amplifier());
        }
    }

    /**
     * Spawns configured mob entities at resolved ability center.
     */
    private static void applySummon(AbilityContext context, Vec3 center, AbilityEffect effect) {
        if (context == null || context.caster() == null || context.caster().level().isClientSide()) {
            return;
        }
        if (!(context.caster().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ResourceLocation entityId = ResourceLocation.tryParse(effect.id());
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == EntityType.PIG) {
            return;
        }

        if (type.create(serverLevel) instanceof Mob mob) {
            mob.moveTo(center.x, center.y, center.z, context.caster().getYRot(), context.caster().getXRot());
            serverLevel.addFreshEntity(mob);
        }
    }

    /**
     * Spawns an arrow projectile using effect scalars for speed and damage tuning.
     */
    private static void applyProjectile(AbilityContext context, AbilityEffect effect) {
        if (context == null || context.caster() == null || context.caster().level().isClientSide()) {
            return;
        }
        if (!(context.caster().level() instanceof ServerLevel serverLevel)) {
            return;
        }

        AbstractArrow arrow = (AbstractArrow) EntityType.ARROW.create(serverLevel);
        if (arrow == null) {
            return;
        }

        arrow.setOwner(context.caster());
        arrow.setPos(context.caster().getX(), context.caster().getEyeY() - 0.1D, context.caster().getZ());
        arrow.shootFromRotation(context.caster(), context.caster().getXRot(), context.caster().getYRot(), 0.0F,
                (float) Math.max(0.1D, effect.scalars().getOrDefault("speed", 2.0D)), 0.0F);
        if (effect.value() > 0.0D) {
            arrow.setBaseDamage(effect.value());
        }
        serverLevel.addFreshEntity(arrow);
    }

    /**
     * Performs short-range look-direction teleport with hard safety cap.
     */
    private static void applyTeleport(AbilityContext context, AbilityEffect effect) {
        if (context == null || context.caster() == null || context.caster().level().isClientSide()) {
            return;
        }
        if (!(context.caster().level() instanceof ServerLevel)) {
            return;
        }

        double rawDistance = effect.scalars().getOrDefault("distance", Math.max(2.0D, effect.value()));
        double distance = Math.min(MAX_TELEPORT_DISTANCE, Math.max(0.5D, rawDistance));
        Vec3 look = context.caster().getLookAngle().normalize();

        double targetX = context.caster().getX() + (look.x * distance);
        double targetY = context.caster().getY();
        double targetZ = context.caster().getZ() + (look.z * distance);

        context.caster().teleportTo(targetX, targetY, targetZ);
    }
}
