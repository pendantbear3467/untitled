package com.extremecraft.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class RunicGolemEntity extends AbstractECMonster {
    public RunicGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        if (this.tickCount % 200 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
            this.emitAbilityParticles(ParticleTypes.ENCHANT, 14, 0.45D, 0.02D);
        }

        if (this.tickCount % 120 == 0 && this.hasTargetInRange(4.8D)) {
            slamAttack();
        }

        if (this.tickCount % 180 == 0) {
            shockwave();
        }
    }

    private void slamAttack() {
        List<LivingEntity> impacted = this.livingTargetsInRange(4.2D).stream()
                .filter(entity -> entity != this && entity != this.getTarget())
                .toList();

        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            target.hurt(this.damageSources().mobAttack(this), (float) (this.attackDamage() * 1.3D));
            knockbackFromCenter(target, 1.1F, 0.25D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }

        for (LivingEntity entity : impacted) {
            entity.hurt(this.damageSources().mobAttack(this), (float) (this.attackDamage() * 0.75D));
            knockbackFromCenter(entity, 0.8F, 0.18D);
        }

        this.emitAbilityParticles(ParticleTypes.CLOUD, 28, 0.85D, 0.01D);
    }

    private void shockwave() {
        for (LivingEntity entity : this.livingTargetsInRange(6.0D)) {
            if (entity == this) {
                continue;
            }
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 0));
            knockbackFromCenter(entity, 0.45F, 0.1D);
        }

        this.emitAbilityParticles(ParticleTypes.ELECTRIC_SPARK, 24, 1.0D, 0.03D);
    }

    private void knockbackFromCenter(LivingEntity target, float strength, double yBoost) {
        Vec3 delta = target.position().subtract(this.position());
        Vec3 flat = new Vec3(delta.x, 0.0D, delta.z);
        if (flat.lengthSqr() < 1.0E-4D) {
            flat = new Vec3(0.0D, 0.0D, 1.0D);
        }
        Vec3 normalized = flat.normalize().scale(strength);
        target.push(normalized.x, yBoost, normalized.z);
    }
}
