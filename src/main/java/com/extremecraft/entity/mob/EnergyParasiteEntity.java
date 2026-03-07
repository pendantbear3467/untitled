package com.extremecraft.entity.mob;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
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
import net.minecraft.world.item.ItemStack;
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

        int drainInterval = Config.energyParasiteDrainIntervalTicks();
        int drainRadius = Config.energyParasiteDrainRadius();
        int maxDrain = Config.energyParasiteMaxDrainPerPulse();

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
            if (target != null) {
                CombatEngine.applyDamage(DamageContext.builder()
                        .attacker(this)
                        .target(target)
                        .damageAmount((float) (this.attackDamage() * 0.7D + 1.5D))
                        .damageType(DamageType.MAGIC)
                        .abilitySource("mob:energy_parasite_drain")
                        .weaponSource(ItemStack.EMPTY)
                        .armorValue(target.getArmorValue())
                        .build());
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
        int drained = 0;
        BlockPos center = this.blockPosition();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -2, -radius), center.offset(radius, 2, radius))) {
            if (drained >= maxTotalDrain) {
                break;
            }

            BlockEntity blockEntity = this.level().getBlockEntity(pos);
            if (blockEntity == null) {
                continue;
            }

            IEnergyStorage storage = blockEntity.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            if (storage == null) {
                continue;
            }

            drained += drainFromStorage(storage, maxTotalDrain - drained);
        }

        return drained;
    }

    private int drainFromStorage(IEnergyStorage storage, int remaining) {
        if (remaining <= 0 || !storage.canExtract()) {
            return 0;
        }

        int perTap = Math.min(80, remaining);
        return Math.max(0, storage.extractEnergy(perTap, false));
    }
}
