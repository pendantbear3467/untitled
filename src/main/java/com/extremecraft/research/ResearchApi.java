package com.extremecraft.research;

import com.extremecraft.ecosystem.core.progression.ProgressionResearchBridge;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ResearchApi {
    static {
        ProgressionResearchBridge.setProvider((carrier, researchId) -> {
            if (!(carrier instanceof Player player)) {
                return false;
            }
            return get(player).map(research -> research.hasResearch(researchId)).orElse(false);
        });
    }

    private ResearchApi() {}

    public static Optional<ResearchCapability> get(Player player) {
        return player.getCapability(ResearchProvider.PLAYER_RESEARCH).resolve();
    }
}
