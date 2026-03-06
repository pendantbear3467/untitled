package com.extremecraft.platform.service;

import net.minecraft.server.level.ServerPlayer;

public interface EnergyNetworkService {
    void sync(ServerPlayer player);
}
