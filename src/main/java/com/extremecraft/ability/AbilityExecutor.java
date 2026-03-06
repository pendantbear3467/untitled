package com.extremecraft.ability;

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
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
        for (LivingEntity target : targets) {
            target.hurt(context.caster().damageSources().playerAttack(context.caster()), damage);
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
