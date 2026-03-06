package com.extremecraft.entity.extension;

import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface EntityExtension {
    void onServerTick(LivingEntity entity);
}
