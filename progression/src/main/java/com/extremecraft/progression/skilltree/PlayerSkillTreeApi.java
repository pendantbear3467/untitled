package com.extremecraft.progression.skilltree;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerSkillTreeApi {
    private PlayerSkillTreeApi() {}

    public static Optional<PlayerSkillData> get(Player player) {
        return player.getCapability(PlayerSkillDataProvider.PLAYER_SKILL_DATA).resolve();
    }
}
