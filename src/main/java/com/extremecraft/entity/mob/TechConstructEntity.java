package com.extremecraft.entity.mob;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class TechConstructEntity extends AbstractECMonster {
    public TechConstructEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }
}
