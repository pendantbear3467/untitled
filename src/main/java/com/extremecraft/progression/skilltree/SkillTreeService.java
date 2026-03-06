package com.extremecraft.progression.skilltree;

import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.level.PlayerLevelApi;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

public final class SkillTreeService {
    private SkillTreeService() {
    }

    public static Map<String, List<SkillNode>> loadSkillTrees() {
        return SkillTreeManager.allTrees();
    }

    public static boolean tryUnlock(ServerPlayer player, String treeId, String nodeId) {
        if (player == null || treeId == null || treeId.isBlank() || nodeId == null || nodeId.isBlank()) {
            return false;
        }

        SkillNode node = SkillTreeManager.getNode(nodeId);
        if (node == null) {
            return false;
        }

        boolean inTree = SkillTreeManager.nodesForTree(treeId).stream().anyMatch(n -> n.id().equals(nodeId));
        if (!inTree) {
            return false;
        }

        PlayerStatsCapability stats = PlayerStatsApi.get(player).orElse(null);
        if (stats == null) {
            return false;
        }

        if (!meetsRequirements(player, stats, node)) {
            return false;
        }

        boolean changed = PlayerStatsService.applyUpgradeRequest(player, "skill:" + nodeId);
        if (!changed) {
            return false;
        }

        applyModifiers(player);

        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            data.unlock(treeId, nodeId);
            sync(player, data);
        });

        return true;
    }

    public static boolean meetsRequirements(ServerPlayer player, PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return false;
        }

        int level = PlayerLevelApi.get(player).map(levelData -> levelData.level()).orElse(stats.level());
        if (level < node.requiredLevel()) {
            return false;
        }

        if (stats.skillPoints() < node.cost()) {
            return false;
        }

        for (String req : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(req)) {
                return false;
            }
        }

        return !stats.isSkillUnlocked(node.id());
    }

    public static void applyModifiers(ServerPlayer player) {
        // Stats recalculation and runtime aggregation are handled by PlayerStatsService + StatCalculationEngine.
        PlayerStatsService.sync(player);
        RuntimeSyncService.syncStats(player);
    }

    public static void flushDirty(ServerPlayer player) {
        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            if (data.consumeDirty()) {
                sync(player, data);
            }
        });

        PlayerStatsApi.get(player).ifPresent(stats -> {
            if ((player.tickCount % 20) == 0) {
                PlayerStatsService.sync(player, stats);
            }
        });
    }

    public static void sync(ServerPlayer player, PlayerSkillData data) {
        com.extremecraft.net.DwNetwork.CH.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player), new SyncSkillTreeDataS2C(data.serializeNBT()));
    }
}
