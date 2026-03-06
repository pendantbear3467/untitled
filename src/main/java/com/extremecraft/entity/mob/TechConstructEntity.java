package com.extremecraft.entity.mob;

import com.extremecraft.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class TechConstructEntity extends AbstractECMonster {
    public TechConstructEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent getAttackSoundEvent() {
        return ModSounds.TECH_CONSTRUCT_ATTACK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.TECH_CONSTRUCT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.TECH_CONSTRUCT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.TECH_CONSTRUCT_DEATH.get();
    }
}
