package com.extremecraft.ability;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.BuffStackingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

public final class AbilityExecutor {
    private AbilityExecutor() {
    }

    public static boolean tryActivate(ServerPlayer player, String requestedAbilityId) {
        if (player == null || requestedAbilityId == null || requestedAbilityId.isBlank()) {
            return false;
        }

        AbilityDefinition definition = AbilityRegistry.get(requestedAbilityId);
        if (definition == null) {
            return false;
        }

        if (!ClassAbilityBindings.canUseAbility(player, definition.id(), definition.requiredClass())) {
            return false;
        }

        if (!AbilityCooldownManager.isReady(player, definition.id())) {
            return false;
        }

        if (!ManaService.tryConsume(player, definition.manaCost())) {
            return false;
        }

        AbilityContext context = AbilityContext.of(player, definition);
        AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(context);

        applyEffects(context, targets.entities(), targets.center());
        AbilityCooldownManager.startCooldown(player, definition.id(), definition.cooldownTicks());
        RuntimeSyncService.syncAbilities(player);
        return true;
    }

    public static boolean executeSpellAbility(ServerPlayer player, AbilityDefinition definition) {
        if (player == null || definition == null) {
            return false;
        }

        AbilityContext context = AbilityContext.of(player, definition);
        AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(context);
        applyEffects(context, targets.entities(), targets.center());
        return true;
    }

    public static boolean executeDefinition(AbilityContext context) {
        if (context == null || context.player() == null || context.definition() == null) {
            return false;
        }

        AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(context);
        AbilityContext resolved = context.withTarget(targets.entities().isEmpty() ? null : targets.entities().get(0), targets.center());
        applyEffects(resolved, targets.entities(), targets.center());
        return true;
    }

    public static void executeProjectile(AbilityContext context, AbilityEffect effect) {
        if (context == null || effect == null) {
            return;
        }
        applyProjectile(context, effect);
    }

    public static void executeProjectile(AbilityContext context) {
        executeProjectile(context, new AbilityEffect("projectile", 2.0D, 0, 0, "", Map.of()));
    }

    public static void executeAreaEffect(AbilityContext context, double radius, List<AbilityEffect> effects) {
        if (context == null || context.player() == null || effects == null || effects.isEmpty()) {
            return;
        }

        List<LivingEntity> targets = context.player().level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(context.player().blockPosition()).inflate(Math.max(1.0D, radius)),
                entity -> entity.isAlive() && entity != context.player()
        );

        applyEffects(context, targets, context.player().position(), effects);
    }

    public static void executeMovement(AbilityContext context, double distance) {
        if (context == null || context.player() == null || context.player().level().isClientSide()) {
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

    public static void executeHeal(AbilityContext context, List<? extends LivingEntity> targets, AbilityEffect effect) {
        if (effect == null) {
            return;
        }
        applyHeal((List<LivingEntity>) (targets == null ? List.of() : targets), effect);
    }

    public static void executeBuff(AbilityContext context, List<? extends LivingEntity> targets, AbilityEffect effect, boolean harmful) {
        if (effect == null) {
            return;
        }
        applyBuff((List<LivingEntity>) (targets == null ? List.of() : targets), effect, harmful);
    }

    private static void applyEffects(AbilityContext context, List<LivingEntity> targets, Vec3 center) {
        for (AbilityEffect effect : context.definition().effects()) {
            switch (effect.type()) {
                case "damage" -> applyDamage(context, targets, effect);
                case "ignite" -> applyIgnite(targets, effect);
                case "heal" -> applyHeal(targets.isEmpty() ? List.of(context.caster()) : targets, effect);
                case "buff" -> applyBuff(targets.isEmpty() ? List.of(context.caster()) : targets, effect, false);
                case "debuff" -> applyBuff(targets, effect, true);
                case "summon" -> applySummon(context, center, effect);
                case "projectile" -> applyProjectile(context, effect);
                case "teleport" -> applyTeleport(context, effect);
                default -> {
                }
            }
        }
    }

    private static void applyDamage(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect) {
        if (targets.isEmpty()) {
            return;
        }

        float damage = (float) Math.max(0.0D, effect.value());
        DamageType damageType = switch ((effect.id() == null ? "" : effect.id()).trim().toLowerCase()) {
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
                    .abilitySource(context.definition().id())
                    .weaponSource(context.caster().getMainHandItem())
                    .armorValue(target.getArmorValue())
                    .build());
        }
    }

    private static void applyIgnite(List<LivingEntity> targets, AbilityEffect effect) {
        int seconds = Math.max(1, effect.duration());
        for (LivingEntity target : targets) {
            target.setSecondsOnFire(seconds);
        }
    }

    private static void applyHeal(List<LivingEntity> targets, AbilityEffect effect) {
        float amount = (float) Math.max(0.0D, effect.value());
        for (LivingEntity target : targets) {
            target.heal(amount);
        }
    }

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
        MobEffectInstance instance = new MobEffectInstance(mobEffect, durationTicks, effect.amplifier());
        for (LivingEntity target : targets) {
            target.addEffect(instance);
            BuffStackingSystem.track(target, effect.type() + ":" + effect.id(), durationTicks, effect.amplifier());
        }
    }

    private static void applySummon(AbilityContext context, Vec3 center, AbilityEffect effect) {
        ResourceLocation entityId = ResourceLocation.tryParse(effect.id());
        if (entityId == null) {
            return;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(entityId);
        if (type == null || type == EntityType.PIG) {
            return;
        }

        if (type.create(context.caster().level()) instanceof Mob mob) {
            mob.moveTo(center.x, center.y, center.z, context.caster().getYRot(), context.caster().getXRot());
            context.caster().level().addFreshEntity(mob);
        }
    }

    private static void applyProjectile(AbilityContext context, AbilityEffect effect) {
        if (context.caster().level().isClientSide()) {
            return;
        }

        var arrow = EntityType.ARROW.create(context.caster().level());
        if (arrow == null) {
            return;
        }

        arrow.setPos(context.caster().getX(), context.caster().getEyeY() - 0.1D, context.caster().getZ());
        arrow.shootFromRotation(context.caster(), context.caster().getXRot(), context.caster().getYRot(), 0.0F,
                (float) Math.max(0.1D, effect.scalars().getOrDefault("speed", 2.0D)), 0.0F);
        context.caster().level().addFreshEntity(arrow);
    }

    private static void applyTeleport(AbilityContext context, AbilityEffect effect) {
        double distance = effect.scalars().getOrDefault("distance", Math.max(2.0D, effect.value()));
        Vec3 look = context.caster().getLookAngle().normalize();

        double targetX = context.caster().getX() + (look.x * distance);
        double targetY = context.caster().getY();
        double targetZ = context.caster().getZ() + (look.z * distance);

        context.caster().teleportTo(targetX, targetY, targetZ);
    }
}
