package com.extremecraft.combat.dualwield;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerDualWieldApi {
    private PlayerDualWieldApi() {}

    public static Optional<PlayerDualWieldData> get(Player player) {
        return player.getCapability(PlayerDualWieldProvider.PLAYER_DUAL_WIELD).resolve();
    }
}
