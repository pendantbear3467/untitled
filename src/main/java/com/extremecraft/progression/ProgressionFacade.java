package com.extremecraft.progression;

import com.extremecraft.ecosystem.core.progression.ProgressionReadAccess;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.quest.QuestDefinition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;

/**
 * Canonical cross-system facade for progression mutations and progression-owned rewards.
 *
 * <p>Gameplay systems should call this facade instead of mutating progression capabilities or
 * mirrors directly.</p>
 */
public final class ProgressionFacade {
    private static final ProgressionReadAccess READ_ACCESS = new ReadAccessImpl();
    private static final InternalProgressionWriteAccess WRITE_ACCESS = new WriteAccessImpl();

    private ProgressionFacade() {
    }

    public static ProgressionReadAccess readAccess() {
        return READ_ACCESS;
    }

    public static int grantPlayerXp(ServerPlayer player, int amount) {
        return WRITE_ACCESS.grantPlayerXp(player, amount);
    }

    public static int grantCombatSkillXp(ServerPlayer player, LivingEntity target) {
        return WRITE_ACCESS.grantCombatSkillXp(player, target);
    }

    public static int grantSkillXp(ServerPlayer player, String skillId, int amount, SkillProgressionService.Source source) {
        return WRITE_ACCESS.grantSkillXp(player, skillId, amount, source);
    }

    public static int grantClassXp(ServerPlayer player, String classId, int amount, ClassProgressionService.Source source) {
        return WRITE_ACCESS.grantClassXp(player, classId, amount, source);
    }

    public static boolean addPlayerSkillPoints(ServerPlayer player, int amount) {
        return WRITE_ACCESS.addPlayerSkillPoints(player, amount);
    }

    public static boolean consumePlayerSkillPoints(ServerPlayer player, int amount) {
        return WRITE_ACCESS.consumePlayerSkillPoints(player, amount);
    }

    public static boolean addClassSkillPoints(ServerPlayer player, int amount) {
        return WRITE_ACCESS.addClassSkillPoints(player, amount);
    }

    public static boolean unlockClass(ServerPlayer player, String classId) {
        return WRITE_ACCESS.unlockClass(player, classId);
    }

    public static void grantUnlock(ServerPlayer player, String unlockId) {
        WRITE_ACCESS.grantUnlock(player, unlockId);
    }

    public static void grantUnlocks(ServerPlayer player, Collection<String> unlockIds) {
        WRITE_ACCESS.grantUnlocks(player, unlockIds);
    }

    public static boolean addQuestProgress(ServerPlayer player, String questId, int amount) {
        return WRITE_ACCESS.addQuestProgress(player, questId, amount);
    }

    public static boolean markQuestCompleted(ServerPlayer player, String questId) {
        return WRITE_ACCESS.markQuestCompleted(player, questId);
    }

    public static boolean discoverRegion(ServerPlayer player, String regionKey) {
        return WRITE_ACCESS.discoverRegion(player, regionKey);
    }

    public static boolean grantStage(ServerPlayer player, String stageId) {
        return WRITE_ACCESS.grantStage(player, stageId);
    }

    public static boolean claimGuildQuestReward(ServerPlayer player, QuestDefinition quest) {
        return WRITE_ACCESS.claimGuildQuestReward(player, quest);
    }

    public static boolean switchClass(ServerPlayer player, String classId) {
        return WRITE_ACCESS.switchClass(player, classId);
    }

    private static final class ReadAccessImpl implements ProgressionReadAccess {
        @Override
        public int level(ServerPlayer player) {
            return ProgressApi.get(player).map(PlayerProgressData::level).orElse(1);
        }

        @Override
        public int xp(ServerPlayer player) {
            return ProgressApi.get(player).map(PlayerProgressData::xp).orElse(0);
        }

        @Override
        public int playerSkillPoints(ServerPlayer player) {
            return ProgressApi.get(player).map(PlayerProgressData::playerSkillPoints).orElse(0);
        }

        @Override
        public int classSkillPoints(ServerPlayer player) {
            return ProgressApi.get(player).map(PlayerProgressData::classSkillPoints).orElse(0);
        }

        @Override
        public int questProgress(ServerPlayer player, String questId) {
            return ProgressionService.getQuestProgress(player, questId);
        }

        @Override
        public boolean questCompleted(ServerPlayer player, String questId) {
            return ProgressionService.isQuestCompleted(player, questId);
        }

        @Override
        public String currentClass(ServerPlayer player) {
            return ProgressApi.get(player).map(PlayerProgressData::currentClass).orElse("warrior");
        }

        @Override
        public int classLevel(ServerPlayer player, String classId) {
            return ProgressApi.get(player).map(data -> data.getClassLevel(classId)).orElse(1);
        }

        @Override
        public int classXp(ServerPlayer player, String classId) {
            return ProgressApi.get(player).map(data -> data.getClassExperience(classId)).orElse(0);
        }

        @Override
        public boolean hasUnlock(ServerPlayer player, String unlockId) {
            return ProgressApi.get(player).map(data -> data.hasUnlock(unlockId)).orElse(false);
        }
    }

    private static final class WriteAccessImpl implements InternalProgressionWriteAccess {
        @Override
        public int grantPlayerXp(ServerPlayer player, int amount) {
            return ProgressionMutationService.grantXp(player, amount);
        }

        @Override
        public int grantCombatSkillXp(ServerPlayer player, LivingEntity target) {
            return SkillProgressionService.grantCombatKillXp(player, target);
        }

        @Override
        public int grantSkillXp(ServerPlayer player, String skillId, int amount, SkillProgressionService.Source source) {
            return SkillProgressionService.grantSkillXp(player, skillId, amount, source);
        }

        @Override
        public int grantClassXp(ServerPlayer player, String classId, int amount, ClassProgressionService.Source source) {
            return ClassProgressionService.grantClassXp(player, classId, amount, source);
        }

        @Override
        public boolean addPlayerSkillPoints(ServerPlayer player, int amount) {
            return ProgressionService.addPlayerSkillPoints(player, amount);
        }

        @Override
        public boolean consumePlayerSkillPoints(ServerPlayer player, int amount) {
            return ProgressionService.consumePlayerSkillPoints(player, amount);
        }

        @Override
        public boolean addClassSkillPoints(ServerPlayer player, int amount) {
            return ProgressionService.addClassSkillPoints(player, amount);
        }

        @Override
        public boolean unlockClass(ServerPlayer player, String classId) {
            return ProgressionService.unlockClass(player, classId);
        }

        @Override
        public void grantUnlock(ServerPlayer player, String unlockId) {
            ProgressionService.grantUnlock(player, unlockId);
        }

        @Override
        public void grantUnlocks(ServerPlayer player, Collection<String> unlockIds) {
            ProgressionService.grantUnlocks(player, unlockIds);
        }

        @Override
        public boolean addQuestProgress(ServerPlayer player, String questId, int amount) {
            return ProgressionService.addQuestProgress(player, questId, amount);
        }

        @Override
        public boolean markQuestCompleted(ServerPlayer player, String questId) {
            return ProgressionService.markQuestCompleted(player, questId);
        }

        @Override
        public boolean discoverRegion(ServerPlayer player, String regionKey) {
            return ProgressionService.discoverRegion(player, regionKey);
        }

        @Override
        public boolean grantStage(ServerPlayer player, String stageId) {
            return ProgressionGate.grantStage(player, stageId);
        }

        @Override
        public boolean claimGuildQuestReward(ServerPlayer player, QuestDefinition quest) {
            return QuestRewardService.claimQuestReward(player, quest);
        }

        @Override
        public boolean switchClass(ServerPlayer player, String classId) {
            return ProgressionService.switchClass(player, classId);
        }
    }
}
