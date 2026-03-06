package com.extremecraft.entity.mob;

import com.extremecraft.config.Config;
import com.extremecraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyParasiteEntity extends AbstractECMonster {
    public EnergyParasiteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        int drainInterval = Math.max(10, Config.COMMON.mobs.energyParasiteDrainIntervalTicks.get());
        int drainRadius = Math.max(1, Config.COMMON.mobs.energyParasiteDrainRadius.get());
        int maxDrain = Math.max(0, Config.COMMON.mobs.energyParasiteMaxDrainPerPulse.get());

        if (maxDrain > 0 && this.tickCount % drainInterval == 0) {
            int drained = drainNearbyMachines(drainRadius, maxDrain);
            if (drained > 0) {
                float healAmount = Math.min(6.0F, drained / 120.0F);
                this.heal(healAmount);
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 0));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
                this.emitAbilityParticles(ParticleTypes.ELECTRIC_SPARK, 14, 0.45D, 0.02D);
            }
        }

        if (this.tickCount % 70 == 0 && this.hasTargetInRange(5.0D)) {
            LivingEntity target = this.getTarget();
            if (target != null && target.hurt(this.damageSources().magic(), (float) (this.attackDamage() * 0.7D + 1.5D))) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                this.emitAbilityParticles(ParticleTypes.WITCH, 12, 0.35D, 0.02D);
            }
        }
    }

    @Override
    protected SoundEvent getAttackSoundEvent() {
        return ModSounds.ENERGY_PARASITE_ATTACK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ENERGY_PARASITE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENERGY_PARASITE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENERGY_PARASITE_DEATH.get();
    }

    private int drainNearbyMachines(int radius, int maxTotalDrain) {
        int[] drained = new int[]{0};
        BlockPos center = this.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (drained[0] >= maxTotalDrain) {
                break;
            }

            BlockEntity blockEntity = this.level().getBlockEntity(pos);
            if (blockEntity == null) {
                continue;
            }

            blockEntity.getCapability(ForgeCapabilities.ENERGY).ifPresent(storage -> drained[0] += drainFromStorage(storage, maxTotalDrain - drained[0]));
        }

        return drained[0];
    }

    private int drainFromStorage(IEnergyStorage storage, int remaining) {
        if (remaining <= 0 || !storage.canExtract()) {
            return 0;
        }

        int perTap = Math.min(80, remaining);
        return Math.max(0, storage.extractEnergy(perTap, false));
    }
}
