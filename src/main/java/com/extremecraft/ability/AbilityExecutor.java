package com.extremecraft.ability;

import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.progression.BuffStackingSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public final class AbilityExecutor {
    private static final Logger LOGGER = LogManager.getLogger();

    private AbilityExecutor() {
    }

    public static boolean tryActivate(ServerPlayer player, String requestedAbilityId) {
        return AbilityEngine.cast(player, requestedAbilityId).succeeded();
    }

    public static boolean executeDefinition(AbilityContext context) {
        if (context == null || context.player() == null || context.definition() == null) {
            return false;
        }

        AbilityDefinition definition = context.definition();
        if (definition.effects().isEmpty()) {
            return false;
        }

        AbilityTargetResolver.TargetBundle targets = AbilityTargetResolver.resolve(context);
        LivingEntity primary = targets.entities().isEmpty() ? null : targets.entities().get(0);
        AbilityContext resolvedContext = context.withTarget(primary, targets.center());
        applyEffects(resolvedContext, targets.entities(), targets.center(), definition.effects());
        return true;
    }

    public static void executeProjectile(AbilityContext context) {
        executeProjectile(context, new AbilityEffect("projectile", 2.0D, 0, 0, "", Map.of()));
    }

    public static void executeProjectile(AbilityContext context, AbilityEffect effect) {
        applyProjectile(context, effect);
    }

    public static void executeAreaEffect(AbilityContext context, double radius, List<AbilityEffect> effects) {
        if (context == null || context.player() == null || effects == null || effects.isEmpty()) {
            return;
        }

        double effectiveRadius = Math.max(1.0D, radius);
        List<LivingEntity> targets = context.player().level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(context.player().blockPosition()).inflate(effectiveRadius),
                entity -> entity.isAlive() && entity != context.player()
        );

        applyEffects(context, targets, context.player().position(), effects);
    }

    public static void executeBuff(AbilityContext context, AbilityEffect effect, boolean harmful) {
        if (context == null || context.player() == null || effect == null) {
            return;
        }

        List<LivingEntity> targets = context.target() == null ? List.of(context.player()) : List.of(context.target());
        executeBuff(context, targets, effect, harmful);
    }

    public static void executeBuff(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect, boolean harmful) {
        applyBuff(context, targets, effect, harmful);
    }

    public static void executeHeal(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect) {
        applyHeal(context, targets, effect);
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
        context.player().swing(InteractionHand.MAIN_HAND, true);
    }

    public static void executeSummon(AbilityContext context, AbilityEffect effect) {
        if (context == null) {
            return;
        }

        Vec3 center = context.target() == null ? context.player().position() : context.target().position();
        applySummon(context, center, effect);
    }

    public static void executeChannel(AbilityContext context, AbilityEffect effect) {
        if (context == null || effect == null) {
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

    private static String resolveChannelPulseType(AbilityEffect effect) {
        String id = effect.id();
        if ("heal".equals(id) || "ignite".equals(id) || "buff".equals(id) || "debuff".equals(id) || "projectile".equals(id)) {
            return id;
        }
        return "damage";
    }

    private static void applyEffects(AbilityContext context, List<LivingEntity> targets, Vec3 center, List<AbilityEffect> effects) {
        for (AbilityEffect effect : effects) {
            applyEffect(context, targets, center, effect);
        }
    }

    private static void applyEffect(AbilityContext context, List<LivingEntity> targets, Vec3 center, AbilityEffect effect) {
        switch (effect.type()) {
            case "damage" -> applyDamage(context, targets, effect);
            case "ignite" -> applyIgnite(targets, effect);
            case "heal" -> applyHeal(context, targets, effect);
            case "buff" -> applyBuff(context, targets, effect, false);
            case "debuff" -> applyBuff(context, targets, effect, true);
            case "summon" -> applySummon(context, center, effect);
            case "projectile" -> applyProjectile(context, effect);
            case "move", "movement", "dash" -> executeMovement(context, effect.value());
            case "channel" -> executeChannel(context, effect);
            default -> {
            }
        }
    }

    private static void applyDamage(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        float damage = (float) Math.max(0.0D, effect.value());
        for (LivingEntity target : targets) {
            target.hurt(context.player().damageSources().playerAttack(context.player()), damage);
        }
    }

    private static void applyIgnite(List<LivingEntity> targets, AbilityEffect effect) {
        if (targets == null || targets.isEmpty()) {
            return;
        }

        int seconds = Math.max(1, effect.duration());
        for (LivingEntity target : targets) {
            target.setSecondsOnFire(seconds);
        }
    }

    private static void applyHeal(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect) {
        List<LivingEntity> recipients = (targets == null || targets.isEmpty()) ? List.of(context.player()) : targets;
        float amount = (float) Math.max(0.0D, effect.value());
        for (LivingEntity target : recipients) {
            target.heal(amount);
        }
    }

    private static void applyBuff(AbilityContext context, List<LivingEntity> targets, AbilityEffect effect, boolean harmful) {
        if (effect == null) {
            return;
        }

        ResourceLocation effectId = ResourceLocation.tryParse(effect.id());
        if (effectId == null) {
            return;
        }

        MobEffect mobEffect = BuiltInRegistries.MOB_EFFECT.get(effectId);
        if (mobEffect == null || mobEffect.isBeneficial() == harmful) {
            return;
        }

        List<LivingEntity> recipients = (targets == null || targets.isEmpty()) ? List.of(context.player()) : targets;
        int durationTicks = Math.max(20, effect.duration() * 20);
        MobEffectInstance instance = new MobEffectInstance(mobEffect, durationTicks, effect.amplifier());
        for (LivingEntity target : recipients) {
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

        if (type.create(context.player().level()) instanceof Mob mob) {
            mob.moveTo(center.x, center.y, center.z, context.player().getYRot(), context.player().getXRot());
            context.player().level().addFreshEntity(mob);
        }
    }

    private static void applyProjectile(AbilityContext context, AbilityEffect effect) {
        if (context == null || context.player() == null || context.player().level().isClientSide()) {
            return;
        }

        AbstractArrow arrow = (AbstractArrow) EntityType.ARROW.create(context.player().level());
        if (arrow == null) {
            return;
        }

        arrow.setOwner(context.player());
        arrow.setPos(context.player().getX(), context.player().getEyeY() - 0.1D, context.player().getZ());

        float velocity = (float) Math.max(0.1D, effect.scalars().getOrDefault("speed", 2.0D));
        arrow.shootFromRotation(context.player(), context.player().getXRot(), context.player().getYRot(), 0.0F, velocity, 0.0F);
        if (effect.value() > 0.0D) {
            arrow.setBaseDamage(effect.value());
        }

        context.player().level().addFreshEntity(arrow);
    }
}
