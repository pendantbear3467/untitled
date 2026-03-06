package com.extremecraft.entity.mob;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class EnergyParasiteEntity extends AbstractECMonster {
    public EnergyParasiteEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tickCount % 100 == 0 && this.hasTargetInRange(5.0D)) {
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 80, 0));
        }
    }
}
