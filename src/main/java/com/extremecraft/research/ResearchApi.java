package com.extremecraft.research;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ResearchApi {
    private ResearchApi() {}

    public static Optional<ResearchCapability> get(Player player) {
        return player.getCapability(ResearchProvider.PLAYER_RESEARCH).resolve();
    }
}
