package com.extremecraft.progression.skilltree;

import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.PlayerStatsApi;
import net.minecraft.server.level.ServerPlayer;

public final class SkillTreeService {
    private SkillTreeService() {
    }

    public static boolean tryUnlock(ServerPlayer player, String treeId, String nodeId) {
        if (treeId == null || treeId.isBlank() || nodeId == null || nodeId.isBlank()) {
            return false;
        }

        SkillNode node = SkillTreeManager.getNode(nodeId);
        if (node == null) {
            return false;
        }

        // Validate tree ownership so clients cannot unlock nodes outside the selected tree.
        boolean inTree = SkillTreeManager.nodesForTree(treeId).stream().anyMatch(n -> n.id().equals(nodeId));
        if (!inTree) {
            return false;
        }

        boolean changed = PlayerStatsService.applyUpgradeRequest(player, "skill:" + nodeId);
        if (!changed) {
            return false;
        }

        // Keep legacy skill data synchronized for compatibility paths.
        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            data.unlock(treeId, nodeId);
            sync(player, data);
        });

        return true;
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
