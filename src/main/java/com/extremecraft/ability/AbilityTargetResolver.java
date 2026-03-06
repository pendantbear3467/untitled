package com.extremecraft.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class AbilityTargetResolver {
    public record TargetBundle(List<LivingEntity> entities, Vec3 center) {
    }

    private AbilityTargetResolver() {
    }

    public static TargetBundle resolve(AbilityContext context) {
        if (context == null || context.player() == null) {
            return new TargetBundle(List.of(), Vec3.ZERO);
        }

        AbilityDefinition definition = context.definition();
        if (definition == null) {
            if (context.target() != null) {
                return new TargetBundle(List.of(context.target()), context.target().position());
            }
            return new TargetBundle(List.of(), context.position());
        }

        return switch (definition.targetType()) {
            case SELF -> new TargetBundle(List.of(context.player()), context.player().position());
            case ENTITY -> resolveEntityTarget(context);
            case AREA -> resolveAreaTarget(context);
            case PROJECTILE -> resolveProjectileTarget(context);
            case NONE -> new TargetBundle(List.of(), context.player().position());
        };
    }

    private static TargetBundle resolveEntityTarget(AbilityContext context) {
        Vec3 start = context.player().getEyePosition();
        Vec3 end = start.add(context.player().getLookAngle().scale(context.definition().range()));
        AABB bounds = context.player().getBoundingBox()
                .expandTowards(context.player().getLookAngle().scale(context.definition().range()))
                .inflate(1.0D);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                context.player().level(),
                context.player(),
                start,
                end,
                bounds,
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != context.player()
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return new TargetBundle(List.of(living), living.position());
        }

        return new TargetBundle(List.of(), end);
    }

    private static TargetBundle resolveAreaTarget(AbilityContext context) {
        double radius = Math.max(1.0D, context.definition().radius());
        Vec3 center = context.position() == null ? context.player().position() : context.position();
        List<LivingEntity> entities = context.player().level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(center, center).inflate(radius),
                entity -> entity.isAlive() && entity != context.player()
        );

        return new TargetBundle(List.copyOf(entities), center);
    }

    private static TargetBundle resolveProjectileTarget(AbilityContext context) {
        Vec3 start = context.player().getEyePosition();
        Vec3 end = start.add(context.player().getLookAngle().scale(context.definition().range()));

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                context.player().level(),
                context.player(),
                start,
                end,
                context.player().getBoundingBox().expandTowards(context.player().getLookAngle().scale(context.definition().range())).inflate(1.0D),
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != context.player()
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return new TargetBundle(List.of(living), living.position());
        }

        return new TargetBundle(List.of(), end);
    }
}
