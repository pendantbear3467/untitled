package com.extremecraft.progression.skilltree;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.level.PlayerLevelApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SkillTreeService {
    /**
     * Canonical service boundary for live skill-tree topology reads and node-unlock mutations.
     *
     * <p>Tree topology comes from {@link SkillTreeManager}. Node unlocks must route through this
     * service so requirement checks and point spend stay aligned with progression state.</p>
     */
    private SkillTreeService() {
    }

    public static Map<String, List<SkillNode>> loadSkillTrees() {
        return SkillTreeManager.allTrees();
    }

    public static boolean tryUnlockByNodeId(ServerPlayer player, String nodeId) {
        if (player == null || nodeId == null || nodeId.isBlank()) {
            return false;
        }

        // Node ids are globally unique; resolve owning tree before applying unlock rules.
        String normalizedNodeId = nodeId.trim().toLowerCase(Locale.ROOT);
        String resolvedTreeId = SkillTreeManager.treeIdForNode(normalizedNodeId);
        if (resolvedTreeId.isBlank()) {
            return false;
        }

        return tryUnlock(player, resolvedTreeId, normalizedNodeId);
    }

    public static boolean tryUnlock(ServerPlayer player, String treeId, String nodeId) {
        if (player == null || treeId == null || treeId.isBlank() || nodeId == null || nodeId.isBlank()) {
            return false;
        }

        String normalizedTreeId = treeId.trim().toLowerCase(Locale.ROOT);
        String normalizedNodeId = nodeId.trim().toLowerCase(Locale.ROOT);

        SkillNode node = SkillTreeManager.getNode(normalizedNodeId);
        if (node == null) {
            return false;
        }

        String ownerTreeId = SkillTreeManager.treeIdForNode(normalizedNodeId);
        if (!normalizedTreeId.equals(ownerTreeId)) {
            return false;
        }

        PlayerStatsCapability stats = PlayerStatsApi.get(player).orElse(null);
        if (stats == null) {
            return false;
        }

        if (!meetsRequirements(player, stats, node)) {
            return false;
        }

        boolean changed = PlayerStatsService.tryUnlockSkillNode(player, normalizedNodeId);
        if (!changed) {
            return false;
        }

        applyModifiers(player);

        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            data.unlock(normalizedTreeId, normalizedNodeId);
            sync(player, data);
        });

        return true;
    }

    public static boolean meetsRequirements(ServerPlayer player, PlayerStatsCapability stats, SkillNode node) {
        if (stats == null || node == null) {
            return false;
        }

        int level = ProgressApi.get(player)
                .map(PlayerProgressData::level)
                .or(() -> PlayerLevelApi.get(player).map(levelData -> levelData.level()))
                .orElse(stats.level());
        if (level < node.requiredLevel()) {
            return false;
        }

        int playerSkillPoints = ProgressApi.get(player)
                .map(PlayerProgressData::playerSkillPoints)
                .orElse(stats.skillPoints());
        if (playerSkillPoints < node.cost()) {
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
        // Dirty flag avoids sending full tree sync packets every tick.
        PlayerSkillTreeApi.get(player).ifPresent(data -> {
            if (data.consumeDirty()) {
                sync(player, data);
            }
        });

        // Stats sync is throttled to keep network traffic bounded during busy combat/progression updates.
        PlayerStatsApi.get(player).ifPresent(stats -> {
            if ((player.tickCount % 20) == 0) {
                PlayerStatsService.sync(player, stats);
            }
        });
    }

    public static void sync(ServerPlayer player, PlayerSkillData data) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncSkillTreeDataS2C(data.serializeNBT()));
    }
}
