package com.extremecraft.skills;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SkillsApi {
    private SkillsApi() {}

    public static Optional<PlayerSkillsCapability> get(Player player) {
        return player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).resolve();
    }
}
