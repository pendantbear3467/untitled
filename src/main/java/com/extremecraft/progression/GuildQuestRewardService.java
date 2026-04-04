package com.extremecraft.progression;

import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical guild quest reward claim path.
 *
 * <p>Quest completion, player XP, class XP, stage grants, unlock grants, and class-unlock rewards
 * should converge here for live guild quest claims.</p>
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
