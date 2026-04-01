package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical guild quest reward claim path.
 *
 * <p>Quest completion, player XP, class XP, stage grants, unlock grants, and class-unlock rewards
 * should converge here for live guild quest claims.</p>
 */
public final class GuildQuestRewardService {
    private GuildQuestRewardService() {
    }

    public static boolean claimQuestReward(ServerPlayer player, QuestDefinition quest) {
        if (player == null || quest == null) {
            return false;
        }

        return ProgressApi.get(player).map(data -> {
            if (ProgressionService.isQuestCompleted(player, quest.id())) {
                return false;
            }

            if (ProgressionService.getQuestProgress(player, quest.id()) < quest.target()) {
                return false;
            }

            ProgressionService.markQuestCompleted(player, quest.id(), false);
            ProgressionService.grantUnlock(player, "quest:" + quest.id(), false);
            ProgressionMutationService.grantXp(player, quest.rewardXp());
            ProgressionService.addPlayerSkillPoints(player, quest.rewardPlayerSkillPoints(), false);
            ProgressionService.addClassSkillPoints(player, quest.rewardClassSkillPoints(), false);

            if (!quest.rewardUnlockClass().isBlank()) {
                ProgressionService.unlockClass(player, quest.rewardUnlockClass(), false);
            }

            String classRewardTarget = !quest.rewardUnlockClass().isBlank() ? quest.rewardUnlockClass() : data.currentClass();
            int classXpReward = calculateClassXpReward(quest);
            // Class XP for live gameplay is intentionally routed through the guild quest claim path.
            ClassProgressionService.grantClassXp(player, classRewardTarget, classXpReward, ClassProgressionService.Source.GUILD_QUEST);

            if (!quest.rewardUnlockStage().isBlank()) {
                ProgressionFacade.grantStage(player, quest.rewardUnlockStage());
            }

            PlayerStatsService.syncProgressionMirror(player, true);
            ProgressionService.flushDirty(player);
            return true;
        }).orElse(false);
    }

    public static int calculateClassXpReward(QuestDefinition quest) {
        if (quest == null) {
            return 0;
        }

        int classPointWeight = Math.max(0, quest.rewardClassSkillPoints()) * 120;
        int baseReward = Math.max(30, quest.rewardXp() / 3);
        return classPointWeight + baseReward;
    }
}
