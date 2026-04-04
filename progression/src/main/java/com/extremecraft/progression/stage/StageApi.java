package com.extremecraft.progression.stage;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class StageApi {
    private StageApi() {}

    public static Optional<PlayerStageData> get(Player player) {
        return player.getCapability(PlayerStageProvider.PLAYER_STAGE).resolve();
    }
}
