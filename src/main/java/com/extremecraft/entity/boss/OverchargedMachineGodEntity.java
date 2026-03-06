package com.extremecraft.entity.boss;

import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class OverchargedMachineGodEntity extends AbstractECBoss {
    public OverchargedMachineGodEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tickCount % 120 == 0) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1));
        }
    }
}
