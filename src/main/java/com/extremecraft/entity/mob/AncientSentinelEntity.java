package com.extremecraft.entity.mob;

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
}
