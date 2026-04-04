package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;

/**
 * Canonical guild quest reward claim path.
 */
final class QuestRewardService {
    private QuestRewardService() {
    }

    static boolean claimQuestReward(ServerPlayer player, QuestDefinition quest) {
        ProgressionMutationAuthority.warnIfBypassed("claimQuestReward");
        if (player == null || quest == null) {
            return false;
        }

        if (ProgressionFacade.readAccess().questCompleted(player, quest.id())) {
            return false;
        }

        if (ProgressionFacade.readAccess().questProgress(player, quest.id()) < quest.target()) {
            return false;
        }

        return ProgressApi.get(player).map(data -> {

            ProgressionFacade.markQuestCompleted(player, quest.id());
            ProgressionFacade.grantUnlock(player, "quest:" + quest.id());
            ProgressionFacade.grantPlayerXp(player, quest.rewardXp());
            ProgressionFacade.addPlayerSkillPoints(player, quest.rewardPlayerSkillPoints());
            ProgressionFacade.addClassSkillPoints(player, quest.rewardClassSkillPoints());

            if (!quest.rewardUnlockClass().isBlank()) {
                ProgressionFacade.unlockClass(player, quest.rewardUnlockClass());
            }

            String classRewardTarget = !quest.rewardUnlockClass().isBlank() ? quest.rewardUnlockClass() : data.currentClass();
            int classXpReward = calculateClassXpReward(quest);
            ProgressionFacade.grantClassXp(player, classRewardTarget, classXpReward, ClassProgressionService.Source.GUILD_QUEST);

            if (!quest.rewardUnlockStage().isBlank()) {
                ProgressionFacade.grantStage(player, quest.rewardUnlockStage());
            }

            PlayerStatsService.syncProgressionMirror(player, true);
            ProgressionSyncService.flush(player);
            return true;
        }).orElse(false);
    }

    static int calculateClassXpReward(QuestDefinition quest) {
        if (quest == null) {
            return 0;
        }

        int classPointWeight = Math.max(0, quest.rewardClassSkillPoints()) * 120;
        int baseReward = Math.max(30, quest.rewardXp() / 3);
        return classPointWeight + baseReward;
    }
}
