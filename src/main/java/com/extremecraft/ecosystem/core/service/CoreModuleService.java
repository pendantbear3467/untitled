package com.extremecraft.ecosystem.core.service;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface CoreModuleService {
    void refresh(ServerPlayer player);
}
