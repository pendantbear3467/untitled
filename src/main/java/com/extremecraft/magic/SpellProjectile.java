package com.extremecraft.magic;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public final class SpellProjectile {
    private final ServerPlayer caster;
    private final double speed;
    private final double maxDistance;

    public SpellProjectile(ServerPlayer caster, double speed, double maxDistance) {
        this.caster = caster;
        this.speed = Math.max(0.1D, speed);
        this.maxDistance = Math.max(1.0D, maxDistance);
    }

    public Optional<LivingEntity> cast() {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(maxDistance));

        AABB bounds = caster.getBoundingBox().expandTowards(caster.getLookAngle().scale(maxDistance)).inflate(Math.max(1.0D, speed * 0.25D));
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                caster.level(),
                caster,
                start,
                end,
                bounds,
                entity -> entity instanceof LivingEntity && entity.isAlive() && entity != caster
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity livingEntity) {
            return Optional.of(livingEntity);
        }

        return Optional.empty();
    }
}
