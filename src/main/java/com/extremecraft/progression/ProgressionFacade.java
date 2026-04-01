package com.extremecraft.progression;

import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Canonical cross-system facade for progression mutations and progression-owned rewards.
 *
 * <p>Gameplay systems should call this facade instead of mutating progression capabilities or
 * mirrors directly.</p>
 */
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

    public static boolean addPlayerSkillPoints(ServerPlayer player, int amount) {
        return ProgressionService.addPlayerSkillPoints(player, amount);
    }

    public static boolean addClassSkillPoints(ServerPlayer player, int amount) {
        return ProgressionService.addClassSkillPoints(player, amount);
    }

    public static boolean unlockClass(ServerPlayer player, String classId) {
        return ProgressionService.unlockClass(player, classId);
    }

    public static void grantUnlock(ServerPlayer player, String unlockId) {
        ProgressionService.grantUnlock(player, unlockId);
    }

    public static void grantUnlocks(ServerPlayer player, java.util.Collection<String> unlockIds) {
        ProgressionService.grantUnlocks(player, unlockIds);
    }

    public static boolean addQuestProgress(ServerPlayer player, String questId, int amount) {
        return ProgressionService.addQuestProgress(player, questId, amount);
    }

    public static boolean markQuestCompleted(ServerPlayer player, String questId) {
        return ProgressionService.markQuestCompleted(player, questId);
    }

    public static boolean discoverRegion(ServerPlayer player, String regionKey) {
        return ProgressionService.discoverRegion(player, regionKey);
    }

    public static boolean grantStage(ServerPlayer player, String stageId) {
        return ProgressionGate.grantStage(player, stageId);
    }

    public static boolean claimGuildQuestReward(ServerPlayer player, QuestDefinition quest) {
        return GuildQuestRewardService.claimQuestReward(player, quest);
    }
}
