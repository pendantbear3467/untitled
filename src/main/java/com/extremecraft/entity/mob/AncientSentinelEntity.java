package com.extremecraft.entity.mob;

import com.extremecraft.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class AncientSentinelEntity extends AbstractECMonster {
    public AncientSentinelEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tickCount % 80 == 0) {
            this.setDeltaMovement(this.getDeltaMovement().x * 0.6D, this.getDeltaMovement().y, this.getDeltaMovement().z * 0.6D);
        }
    }

    @Override
    protected SoundEvent getAttackSoundEvent() {
        return ModSounds.ANCIENT_SENTINEL_ATTACK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ANCIENT_SENTINEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ANCIENT_SENTINEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ANCIENT_SENTINEL_DEATH.get();
    }
}
