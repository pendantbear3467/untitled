package com.extremecraft.entity.mob;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VoidStalkerEntity extends AbstractECMonster {
    public VoidStalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        if (this.tickCount % 180 == 0 && this.distanceToSqr(target) <= 100.0D) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 50, 0, false, false));
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 70, 1));
            this.emitAbilityParticles(ParticleTypes.SMOKE, 16, 0.4D, 0.02D);
        }

        if (this.tickCount % 75 == 0 && this.distanceToSqr(target) <= 25.0D) {
            ambushBehindTarget(target);
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2));
            this.emitAbilityParticles(ParticleTypes.SCULK_SOUL, 12, 0.35D, 0.02D);
        }
    }

    @Override
    public boolean doHurtTarget(Entity entity) {
        boolean hurt = super.doHurtTarget(entity);
        if (!hurt || !(entity instanceof LivingEntity target)) {
            return hurt;
        }

        Vec3 toAttacker = this.position().subtract(target.position()).normalize();
        float facingDot = (float) target.getLookAngle().dot(toAttacker);
        if (facingDot < -0.25F) {
            float bonusDamage = Math.max(2.0F, (float) this.attackDamage() * 0.55F);
            target.hurt(this.damageSources().mobAttack(this), bonusDamage);
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
            this.emitAbilityParticles(ParticleTypes.DRAGON_BREATH, 20, 0.5D, 0.03D);
        }

        return true;
    }

    private void ambushBehindTarget(LivingEntity target) {
        Vec3 backwards = target.getLookAngle().normalize().scale(-1.8D);
        Vec3 ambushPos = target.position().add(backwards);
        float yRot = Mth.wrapDegrees(target.getYRot() + 180.0F);
        this.moveTo(ambushPos.x, ambushPos.y, ambushPos.z, yRot, this.getXRot());
    }
}
