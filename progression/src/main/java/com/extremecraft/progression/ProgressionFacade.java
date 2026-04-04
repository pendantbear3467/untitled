package com.extremecraft.progression;

import com.extremecraft.ecosystem.core.progression.ProgressionReadAccess;
import com.extremecraft.ecosystem.core.progression.ProgressionQuestDescriptor;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.skilltree.SkillTreeService;
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
        return ProgressionMutationAuthority.runInt("grantPlayerXp", () -> WRITE_ACCESS.grantPlayerXp(player, amount));
    }

    public static void setPlayerLevel(ServerPlayer player, int level) {
        ProgressionMutationAuthority.runVoid("setPlayerLevel", () -> WRITE_ACCESS.setPlayerLevel(player, level));
    }

    public static int grantCombatSkillXp(ServerPlayer player, LivingEntity target) {
        return ProgressionMutationAuthority.runInt("grantCombatSkillXp", () -> WRITE_ACCESS.grantCombatSkillXp(player, target));
    }

    public static int grantSkillXp(ServerPlayer player, String skillId, int amount, SkillProgressionService.Source source) {
        return ProgressionMutationAuthority.runInt("grantSkillXp", () -> WRITE_ACCESS.grantSkillXp(player, skillId, amount, source));
    }

    public static int grantClassXp(ServerPlayer player, String classId, int amount, ClassProgressionService.Source source) {
        return ProgressionMutationAuthority.runInt("grantClassXp", () -> WRITE_ACCESS.grantClassXp(player, classId, amount, source));
    }

    public static boolean addPlayerSkillPoints(ServerPlayer player, int amount) {
        return ProgressionMutationAuthority.runBoolean("addPlayerSkillPoints", () -> WRITE_ACCESS.addPlayerSkillPoints(player, amount));
    }

    public static boolean consumePlayerSkillPoints(ServerPlayer player, int amount) {
        return ProgressionMutationAuthority.runBoolean("consumePlayerSkillPoints", () -> WRITE_ACCESS.consumePlayerSkillPoints(player, amount));
    }

    public static boolean addClassSkillPoints(ServerPlayer player, int amount) {
        return ProgressionMutationAuthority.runBoolean("addClassSkillPoints", () -> WRITE_ACCESS.addClassSkillPoints(player, amount));
    }

    public static boolean unlockClass(ServerPlayer player, String classId) {
        return ProgressionMutationAuthority.runBoolean("unlockClass", () -> WRITE_ACCESS.unlockClass(player, classId));
    }

    public static void grantUnlock(ServerPlayer player, String unlockId) {
        ProgressionMutationAuthority.runVoid("grantUnlock", () -> WRITE_ACCESS.grantUnlock(player, unlockId));
    }

    public static void grantUnlocks(ServerPlayer player, Collection<String> unlockIds) {
        ProgressionMutationAuthority.runVoid("grantUnlocks", () -> WRITE_ACCESS.grantUnlocks(player, unlockIds));
    }

    public static boolean addQuestProgress(ServerPlayer player, String questId, int amount) {
        return ProgressionMutationAuthority.runBoolean("addQuestProgress", () -> WRITE_ACCESS.addQuestProgress(player, questId, amount));
    }

    public static boolean markQuestCompleted(ServerPlayer player, String questId) {
        return ProgressionMutationAuthority.runBoolean("markQuestCompleted", () -> WRITE_ACCESS.markQuestCompleted(player, questId));
    }

    public static boolean discoverRegion(ServerPlayer player, String regionKey) {
        return ProgressionMutationAuthority.runBoolean("discoverRegion", () -> WRITE_ACCESS.discoverRegion(player, regionKey));
    }

    public static boolean grantStage(ServerPlayer player, String stageId) {
        return ProgressionMutationAuthority.runBoolean("grantStage", () -> WRITE_ACCESS.grantStage(player, stageId));
    }

    public static boolean claimGuildQuestReward(ServerPlayer player, ProgressionQuestDescriptor quest) {
        return ProgressionMutationAuthority.runBoolean("claimGuildQuestReward", () -> WRITE_ACCESS.claimGuildQuestReward(player, quest));
    }

    public static boolean unlockSkillNodeById(ServerPlayer player, String nodeId) {
        return ProgressionMutationAuthority.runBoolean("unlockSkillNodeById", () -> WRITE_ACCESS.unlockSkillNodeById(player, nodeId));
    }

    public static boolean unlockSkillNode(ServerPlayer player, String treeId, String nodeId) {
        return ProgressionMutationAuthority.runBoolean("unlockSkillNode", () -> WRITE_ACCESS.unlockSkillNode(player, treeId, nodeId));
    }

    public static boolean switchClass(ServerPlayer player, String classId) {
        return ProgressionMutationAuthority.runBoolean("switchClass", () -> WRITE_ACCESS.switchClass(player, classId));
    }

    private static final class ReadAccessImpl implements ProgressionReadAccess {
        @Override
        public int level(Object player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 1;
            }
            return ProgressApi.get(serverPlayer).map(PlayerProgressData::level).orElse(1);
        }

        @Override
        public int xp(Object player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            return ProgressApi.get(serverPlayer).map(PlayerProgressData::xp).orElse(0);
        }

        @Override
        public int playerSkillPoints(Object player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            return ProgressApi.get(serverPlayer).map(PlayerProgressData::playerSkillPoints).orElse(0);
        }

        @Override
        public int classSkillPoints(Object player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            return ProgressApi.get(serverPlayer).map(PlayerProgressData::classSkillPoints).orElse(0);
        }

        @Override
        public int questProgress(Object player, String questId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            return ProgressionService.getQuestProgress(serverPlayer, questId);
        }

        @Override
        public boolean questCompleted(Object player, String questId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            return ProgressionService.isQuestCompleted(serverPlayer, questId);
        }

        @Override
        public String currentClass(Object player) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return "warrior";
            }
            return ProgressApi.get(serverPlayer).map(PlayerProgressData::currentClass).orElse("warrior");
        }

        @Override
        public int classLevel(Object player, String classId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 1;
            }
            return ProgressApi.get(serverPlayer).map(data -> data.getClassLevel(classId)).orElse(1);
        }

        @Override
        public int classXp(Object player, String classId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return 0;
            }
            return ProgressApi.get(serverPlayer).map(data -> data.getClassExperience(classId)).orElse(0);
        }

        @Override
        public boolean hasUnlock(Object player, String unlockId) {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return false;
            }
            return ProgressApi.get(serverPlayer).map(data -> data.hasUnlockGrant(unlockId)).orElse(false);
        }
    }

    private static final class WriteAccessImpl implements InternalProgressionWriteAccess {
        @Override
        public int grantPlayerXp(ServerPlayer player, int amount) {
            return ProgressionMutationService.grantXp(player, amount);
        }

        @Override
        public void setPlayerLevel(ServerPlayer player, int level) {
            ProgressionMutationService.setLevel(player, level);
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
        public boolean claimGuildQuestReward(ServerPlayer player, ProgressionQuestDescriptor quest) {
            return QuestRewardService.claimQuestReward(player, quest);
        }

        @Override
        public boolean unlockSkillNodeById(ServerPlayer player, String nodeId) {
            return SkillTreeService.tryUnlockByNodeId(player, nodeId);
        }

        @Override
        public boolean unlockSkillNode(ServerPlayer player, String treeId, String nodeId) {
            return SkillTreeService.tryUnlock(player, treeId, nodeId);
        }

        @Override
        public boolean switchClass(ServerPlayer player, String classId) {
            return ProgressionService.switchClass(player, classId);
        }
    }
}
