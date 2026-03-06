package com.extremecraft.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class AbilityTargetResolver {
    public record TargetBundle(List<LivingEntity> entities, Vec3 center) {
    }

    private AbilityTargetResolver() {
    }

    public static TargetBundle resolve(AbilityContext context) {
        AbilityDefinition definition = context.definition();
        return switch (definition.targetType()) {
            case SELF -> new TargetBundle(List.of(context.caster()), context.caster().position());
            case ENTITY -> resolveEntityTarget(context);
            case AREA -> resolveAreaTarget(context);
            case PROJECTILE -> resolveProjectileTarget(context);
            case NONE -> new TargetBundle(List.of(), context.caster().position());
        };
    }

    private static TargetBundle resolveEntityTarget(AbilityContext context) {
        Vec3 start = context.caster().getEyePosition();
        Vec3 end = start.add(context.caster().getLookAngle().scale(context.definition().range()));
        AABB bounds = context.caster().getBoundingBox().expandTowards(context.caster().getLookAngle().scale(context.definition().range())).inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                context.caster().level(),
                context.caster(),
                start,
                end,
                bounds,
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != context.caster()
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return new TargetBundle(List.of(living), living.position());
        }

        return new TargetBundle(List.of(), end);
    }

    private static TargetBundle resolveAreaTarget(AbilityContext context) {
        double radius = Math.max(1.0D, context.definition().radius());
        List<LivingEntity> entities = context.caster().level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(context.caster().blockPosition()).inflate(radius),
                entity -> entity.isAlive() && entity != context.caster()
        );

        return new TargetBundle(List.copyOf(entities), context.caster().position());
    }

    private static TargetBundle resolveProjectileTarget(AbilityContext context) {
        Vec3 start = context.caster().getEyePosition();
        Vec3 end = start.add(context.caster().getLookAngle().scale(context.definition().range()));

        List<LivingEntity> entities = new ArrayList<>();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                context.caster().level(),
                context.caster(),
                start,
                end,
                context.caster().getBoundingBox().expandTowards(context.caster().getLookAngle().scale(context.definition().range())).inflate(1.0D),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != context.caster()
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            entities.add(living);
            return new TargetBundle(List.copyOf(entities), living.position());
        }

        return new TargetBundle(List.of(), end);
    }
}
