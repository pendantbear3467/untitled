package com.extremecraft.future;

import net.minecraft.server.level.ServerPlayer;

public interface IDimensionTravelService {
    boolean unlockDimension(ServerPlayer player, String dimensionId);
}
