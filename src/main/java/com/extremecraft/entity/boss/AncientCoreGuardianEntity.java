package com.extremecraft.entity.boss;

import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public final class AncientCoreGuardianEntity extends AbstractECBoss {
    public AncientCoreGuardianEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
    }
}
