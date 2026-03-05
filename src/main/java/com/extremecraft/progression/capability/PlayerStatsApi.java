package com.extremecraft.progression.capability;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerStatsApi {
    private PlayerStatsApi() {
    }

    public static Optional<PlayerStatsCapability> get(Player player) {
        return player.getCapability(PlayerStatsProvider.PLAYER_STATS).resolve();
    }
}
