package com.extremecraft.ecosystem.core.service;

import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface CoreEnergyNetworkService {
    void refresh(ServerPlayer player);
}
