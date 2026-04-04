package com.extremecraft.progression.level;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerLevelApi {
    private PlayerLevelApi() {
    }

    public static Optional<PlayerLevelCapability> get(Player player) {
        return player.getCapability(PlayerLevelProvider.PLAYER_LEVEL).resolve();
    }
}
