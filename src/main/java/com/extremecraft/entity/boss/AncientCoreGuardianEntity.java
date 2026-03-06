package com.extremecraft.entity.boss;

import com.extremecraft.entity.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class AncientCoreGuardianEntity extends AbstractECBoss {
    public AncientCoreGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
    }

    @Override
    protected void performPhaseTick(int phase) {
        if (phase == 1 && this.tickCount % 140 == 0) {
            // Early phase: heavy, durable tank behavior.
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0));
        }

        if (phase >= 2 && this.tickCount % 90 == 0) {
            energyPulse(phase);
        }

        if (phase == 3 && this.tickCount % 70 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
            summonBossMinions(phase);
        }
    }

    @Override
    protected void summonBossMinions(int phase) {
        if (phase == 1) {
            spawnMinions(ModEntities.ANCIENT_SENTINEL.get(), 1, 6.0D);
            return;
        }

        if (phase == 2) {
            spawnMinions(ModEntities.ANCIENT_SENTINEL.get(), 1, 7.0D);
            spawnMinions(ModEntities.ENERGY_PARASITE.get(), 1, 7.0D);
            return;
        }

        spawnMinions(ModEntities.ANCIENT_SENTINEL.get(), 2, 8.0D);
        spawnMinions(ModEntities.ENERGY_PARASITE.get(), 2, 8.0D);
    }

    @Override
    protected float incomingDamageMultiplier() {
        if (currentPhase() == 2) {
            return 0.55F;
        }
        return super.incomingDamageMultiplier();
    }

    private void energyPulse(int phase) {
        float damage = phase >= 3 ? 8.5F : 6.0F;
        this.pulseAreaDamage(6.5D, damage, 0.18D);
        this.pulseParticles(ParticleTypes.ELECTRIC_SPARK, 32, 1.1D, 0.05D);
        this.pulseParticles(ParticleTypes.END_ROD, 18, 1.0D, 0.04D);
    }
}
