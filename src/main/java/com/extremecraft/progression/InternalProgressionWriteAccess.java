package com.extremecraft.progression;

import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

/**
 * Package-restricted progression mutation surface.
 *
 * <p>Write access is intentionally internal to the progression package so other domains are
 * nudged toward facade calls and read-only contracts.</p>
 */
interface InternalProgressionWriteAccess {
    int grantPlayerXp(ServerPlayer player, int amount);

    void setPlayerLevel(ServerPlayer player, int level);

    int grantCombatSkillXp(ServerPlayer player, LivingEntity target);

    int grantSkillXp(ServerPlayer player, String skillId, int amount, SkillProgressionService.Source source);

    int grantClassXp(ServerPlayer player, String classId, int amount, ClassProgressionService.Source source);

    boolean addPlayerSkillPoints(ServerPlayer player, int amount);

    boolean consumePlayerSkillPoints(ServerPlayer player, int amount);

    boolean addClassSkillPoints(ServerPlayer player, int amount);

    boolean unlockClass(ServerPlayer player, String classId);

    void grantUnlock(ServerPlayer player, String unlockId);

    void grantUnlocks(ServerPlayer player, Collection<String> unlockIds);

    boolean addQuestProgress(ServerPlayer player, String questId, int amount);

    boolean markQuestCompleted(ServerPlayer player, String questId);

    boolean discoverRegion(ServerPlayer player, String regionKey);

    boolean grantStage(ServerPlayer player, String stageId);

    boolean claimGuildQuestReward(ServerPlayer player, QuestDefinition quest);

    boolean unlockSkillNodeById(ServerPlayer player, String nodeId);

    boolean unlockSkillNode(ServerPlayer player, String treeId, String nodeId);

    boolean switchClass(ServerPlayer player, String classId);
}
