package com.extremecraft.skills;

import com.extremecraft.ecosystem.core.progression.ProgressionSkillLevelBridge;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class SkillsApi {
    static {
        ProgressionSkillLevelBridge.setProvider((carrier, skillId) -> {
            if (!(carrier instanceof Player player)) {
                return 0;
            }
            return get(player).map(skills -> skills.getSkillLevel(skillId)).orElse(0);
        });
    }

    private SkillsApi() {}

    public static Optional<PlayerSkillsCapability> get(Player player) {
        return player.getCapability(PlayerSkillsProvider.PLAYER_SKILLS).resolve();
    }
}
