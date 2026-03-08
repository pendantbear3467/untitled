package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.progression.stage.StageManager;
import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;

public final class GuildQuestRewardService {
    private GuildQuestRewardService() {
    }

    public static boolean claimQuestReward(ServerPlayer player, QuestDefinition quest) {
        if (player == null || quest == null) {
            return false;
        }

        return ProgressApi.get(player).map(data -> {
            if (data.isQuestCompleted(quest.id())) {
                return false;
            }

            int progress = data.getQuestProgress(quest.id());
            if (progress < quest.target()) {
                return false;
            }

            data.setQuestCompleted(quest.id());
            ProgressionMutationService.grantXp(player, quest.rewardXp());
            data.addPlayerSkillPoints(quest.rewardPlayerSkillPoints());
            data.addClassSkillPoints(quest.rewardClassSkillPoints());

            if (!quest.rewardUnlockClass().isBlank()) {
                data.unlockClass(quest.rewardUnlockClass());
            }

            String classRewardTarget = !quest.rewardUnlockClass().isBlank() ? quest.rewardUnlockClass() : data.currentClass();
            int classXpReward = calculateClassXpReward(quest);
            ClassProgressionService.grantClassXp(player, classRewardTarget, classXpReward, ClassProgressionService.Source.GUILD_QUEST);

            if (!quest.rewardUnlockStage().isBlank()) {
                ProgressionStage.byName(quest.rewardUnlockStage())
                        .ifPresent(stage -> StageManager.upgradePlayerStage(player, stage));
            }

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
