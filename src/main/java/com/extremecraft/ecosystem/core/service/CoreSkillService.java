package com.extremecraft.ecosystem.core.service;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface CoreSkillService {
    void refresh(ServerPlayer player);
}
