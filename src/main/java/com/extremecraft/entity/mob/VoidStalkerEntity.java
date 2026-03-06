package com.extremecraft.entity.mob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class VoidStalkerEntity extends AbstractECMonster {
    public VoidStalkerEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tickCount % 120 == 0 && this.hasTargetInRange(6.0D)) {
            this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
        }
    }
}
