package com.extremecraft.progression;

import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ProgressionFacade {
    private ProgressionFacade() {
    }

    public static int grantPlayerXp(ServerPlayer player, int amount) {
        return ProgressionMutationService.grantXp(player, amount);
    }

    public static int grantCombatSkillXp(ServerPlayer player, LivingEntity target) {
        return SkillProgressionService.grantCombatKillXp(player, target);
    }

    public static int grantSkillXp(ServerPlayer player, String skillId, int amount, SkillProgressionService.Source source) {
        return SkillProgressionService.grantSkillXp(player, skillId, amount, source);
    }

    public static int grantClassXp(ServerPlayer player, String classId, int amount, ClassProgressionService.Source source) {
        return ClassProgressionService.grantClassXp(player, classId, amount, source);
    }

    public static void grantUnlock(ServerPlayer player, String unlockId) {
        ProgressionService.grantUnlock(player, unlockId);
    }

    public static void grantUnlocks(ServerPlayer player, java.util.Collection<String> unlockIds) {
        ProgressionService.grantUnlocks(player, unlockIds);
    }

    public static boolean claimGuildQuestReward(ServerPlayer player, QuestDefinition quest) {
        return GuildQuestRewardService.claimQuestReward(player, quest);
    }
}
