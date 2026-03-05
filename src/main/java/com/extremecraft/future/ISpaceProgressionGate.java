package com.extremecraft.future;

import net.minecraft.server.level.ServerPlayer;

public interface ISpaceProgressionGate {
    boolean canAccessOrbit(ServerPlayer player);
}
