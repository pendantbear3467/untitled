package com.extremecraft.progression.capability;

import com.extremecraft.progression.PlayerProgressData;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ProgressApi {
    private ProgressApi() {}

    public static Optional<PlayerProgressData> get(Player player) {
        return player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS).resolve();
    }

    public static PlayerProgressData getOrCreate(Player player) {
        return get(player).orElseGet(PlayerProgressData::new);
    }
}
