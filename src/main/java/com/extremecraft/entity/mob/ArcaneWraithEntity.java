package com.extremecraft.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class ArcaneWraithEntity extends AbstractECMonster {
    public ArcaneWraithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.tickCount % 60 == 0 && this.hasTargetInRange(8.0D)) {
            this.heal(1.0F);
        }
    }
}
