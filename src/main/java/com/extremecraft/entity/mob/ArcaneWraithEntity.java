package com.extremecraft.entity.mob;

import com.extremecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class ArcaneWraithEntity extends AbstractECMonster {
    public ArcaneWraithEntity(EntityType<? extends Monster> type, Level level) {
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

        if (this.tickCount % 80 == 0 && this.distanceToSqr(target) > 9.0D) {
            teleportNearTarget(target);
        }

        if (this.tickCount % 50 == 0 && this.distanceToSqr(target) <= 16.0D) {
            float lifeDrainDamage = 2.5F + (float) (this.attackDamage() * 0.35D);
            if (target.hurt(this.damageSources().magic(), lifeDrainDamage)) {
                this.heal(Math.max(1.0F, lifeDrainDamage * 0.35F));
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                this.emitAbilityParticles(ParticleTypes.WITCH, 18, 0.45D, 0.06D);
            }
        }

        if (this.tickCount % 140 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.14D, 0.0D));
            this.emitAbilityParticles(ParticleTypes.ENCHANT, 24, 0.55D, 0.05D);
        }
    }

    @Override
    protected SoundEvent getAttackSoundEvent() {
        return ModSounds.ARCANE_WRAITH_ATTACK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ARCANE_WRAITH_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ARCANE_WRAITH_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ARCANE_WRAITH_DEATH.get();
    }

    private void teleportNearTarget(LivingEntity target) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double x = target.getX() + (this.random.nextDouble() - 0.5D) * 7.0D;
            double y = target.getY() + (this.random.nextDouble() - 0.5D) * 2.0D;
            double z = target.getZ() + (this.random.nextDouble() - 0.5D) * 7.0D;
            BlockPos pos = BlockPos.containing(x, y, z);
            if (this.level().getBlockState(pos.below()).isAir()) {
                continue;
            }
            if (this.randomTeleport(x, y, z, true)) {
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.7D), this.getZ(), 28, 0.35D, 0.45D, 0.35D, 0.08D);
                }
                return;
            }
        }
    }
}
