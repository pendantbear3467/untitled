package com.extremecraft.progression;

import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;

/**
 * LEGACY ADAPTER: compatibility alias for {@link QuestRewardService}.
 */
@Deprecated(forRemoval = false, since = "1.2.0")
public final class GuildQuestRewardService {
    private GuildQuestRewardService() {
    }

    public static boolean claimQuestReward(ServerPlayer player, QuestDefinition quest) {
        return QuestRewardService.claimQuestReward(player, quest);
    }

    public static int calculateClassXpReward(QuestDefinition quest) {
        return QuestRewardService.calculateClassXpReward(quest);
    }
}
