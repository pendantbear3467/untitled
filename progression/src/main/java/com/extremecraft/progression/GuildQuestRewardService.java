package com.extremecraft.progression;

import com.extremecraft.ecosystem.core.progression.ProgressionQuestDescriptor;
import net.minecraft.server.level.ServerPlayer;

/**
 * LEGACY ADAPTER: compatibility alias for {@link QuestRewardService}.
 */
@Deprecated(forRemoval = false, since = "1.2.0")
public final class GuildQuestRewardService {
    private GuildQuestRewardService() {
    }

    public static boolean claimQuestReward(ServerPlayer player, ProgressionQuestDescriptor quest) {
        return QuestRewardService.claimQuestReward(player, quest);
    }

    public static int calculateClassXpReward(ProgressionQuestDescriptor quest) {
        return QuestRewardService.calculateClassXpReward(quest);
    }
}
