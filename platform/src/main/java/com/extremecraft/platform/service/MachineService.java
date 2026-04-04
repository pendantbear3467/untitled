package com.extremecraft.platform.service;

import net.minecraft.server.level.ServerPlayer;

public interface MachineService {
    void onMachineTick(ServerPlayer player);
}
