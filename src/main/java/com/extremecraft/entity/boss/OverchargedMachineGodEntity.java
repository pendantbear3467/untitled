package com.extremecraft.entity.boss;

import com.extremecraft.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class OverchargedMachineGodEntity extends AbstractECBoss {
    public OverchargedMachineGodEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
    }

    @Override
    protected void performPhaseTick(int phase) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }

        int laserCadence = phase == 1 ? 95 : (phase == 2 ? 70 : 45);
        if (this.tickCount % laserCadence == 0) {
            fireMachineLaser(target, phase);
        }

        if (phase >= 2 && this.tickCount % 115 == 0) {
            overloadExplosion(phase);
        }

        if (phase == 3 && this.tickCount % 80 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 2));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 1));
            summonBossMinions(phase);
        }
    }

    @Override
    protected void summonBossMinions(int phase) {
        if (phase == 1) {
            spawnMinions(ModEntities.TECH_CONSTRUCT.get(), 1, 7.0D);
            return;
        }

        if (phase == 2) {
            spawnMinions(ModEntities.TECH_CONSTRUCT.get(), 2, 8.0D);
            return;
        }

        spawnMinions(ModEntities.TECH_CONSTRUCT.get(), 3, 9.0D);
        spawnMinions(ModEntities.ENERGY_PARASITE.get(), 1, 9.0D);
    }

    private void fireMachineLaser(LivingEntity target, int phase) {
        float damage = (float) (this.attackDamage() * (phase >= 3 ? 1.25D : 1.0D) + 2.0D);
        target.hurt(this.damageSources().mobAttack(this), damage);
        target.setSecondsOnFire(phase >= 3 ? 3 : 1);

        this.pulseParticles(ParticleTypes.ELECTRIC_SPARK, 34, 0.55D, 0.06D);
        this.pulseParticles(ParticleTypes.SMOKE, 18, 0.45D, 0.02D);
    }

    private void overloadExplosion(int phase) {
        float power = phase >= 3 ? 3.4F : 2.4F;
        this.level().explode(this, this.getX(), this.getY(), this.getZ(), power, Level.ExplosionInteraction.NONE);
        this.pulseAreaDamage(5.5D, phase >= 3 ? 10.0F : 7.0F, 0.25D);
        this.pulseParticles(ParticleTypes.ELECTRIC_SPARK, 40, 1.0D, 0.05D);
    }
}
