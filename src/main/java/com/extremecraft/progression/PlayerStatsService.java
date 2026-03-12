package com.extremecraft.progression;

import com.extremecraft.item.module.ModuleEffectService;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PlayerStatsService {
    private static final int MODULE_SCAN_INTERVAL = 10;

    private PlayerStatsService() {
    }

    public static boolean applyUpgradeRequest(ServerPlayer player, String upgradeId) {
        if (upgradeId == null || upgradeId.isBlank()) {
            return false;
        }

        return PlayerStatsApi.get(player).map(stats -> {
            boolean changed;
            if (upgradeId.startsWith("skill:")) {
                String nodeId = upgradeId.substring("skill:".length());
                changed = unlockSkillNode(player, stats, nodeId);
            } else {
                changed = stats.upgradePrimaryStat(upgradeId);
            }

            if (changed) {
                sync(player, stats);
            }
            return changed;
        }).orElse(false);
    }

    public static boolean addExperience(ServerPlayer player, int amount) {
        return addExperience(player, amount, true);
    }

    public static boolean addExperience(ServerPlayer player, int amount, boolean syncImmediately) {
        return PlayerStatsApi.get(player).map(stats -> {
            boolean leveled = stats.addExperience(amount);
            if (syncImmediately) {
                sync(player, stats);
            }
            return leveled;
        }).orElse(false);
    }

    public static void setLevel(ServerPlayer player, int level) {
        setLevel(player, level, true);
    }

    public static void setLevel(ServerPlayer player, int level, boolean syncImmediately) {
        if (player == null) {
            return;
        }

        PlayerStatsApi.get(player).ifPresent(stats -> {
            stats.setLevel(level);
            if (syncImmediately) {
                sync(player, stats);
            }
        });
    }

    public static void tickResources(ServerPlayer player) {
        PlayerStatsApi.get(player).ifPresent(stats -> {
            boolean moduleEffectsChanged = false;
            if ((player.tickCount % MODULE_SCAN_INTERVAL) == 0) {
                moduleEffectsChanged = ModuleEffectService.applyEquippedModules(player, stats);
            }

            stats.regenerateResources();
            if (moduleEffectsChanged || (player.tickCount % 20) == 0) {
                sync(player, stats);
            }
        });
    }

    public static void sync(ServerPlayer player) {
        PlayerStatsApi.get(player).ifPresent(stats -> sync(player, stats));
    }

    public static void sync(ServerPlayer player, PlayerStatsCapability stats) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerStatsPacket(stats.serializeNBT()));
        RuntimeSyncService.syncStats(player);
        RuntimeSyncService.syncSkillUnlocks(player);
    }

    public static boolean syncProgressionMirror(ServerPlayer player, boolean syncImmediately) {
        if (player == null) {
            return false;
        }

        PlayerStatsCapability stats = PlayerStatsApi.get(player).orElse(null);
        if (stats == null) {
            return false;
        }

        int canonicalSkillPoints = ProgressApi.get(player)
                .map(com.extremecraft.progression.PlayerProgressData::playerSkillPoints)
                .orElse(stats.skillPoints());

        boolean changed = stats.setSkillPoints(canonicalSkillPoints);
        if (changed && syncImmediately) {
            sync(player, stats);
        }
        return changed;
    }

    private static boolean unlockSkillNode(ServerPlayer player, PlayerStatsCapability stats, String nodeId) {
        SkillNode node = SkillTreeManager.getNode(nodeId);
        if (node == null || stats.isSkillUnlocked(node.id())) {
            return false;
        }

        int level = ProgressApi.get(player)
                .map(com.extremecraft.progression.PlayerProgressData::level)
                .orElse(stats.level());
        if (level < node.requiredLevel()) {
            return false;
        }

        for (String req : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(req)) {
                return false;
            }
        }

        com.extremecraft.progression.PlayerProgressData progress = ProgressApi.get(player).orElse(null);
        if (progress == null || !progress.consumePlayerSkillPoints(node.cost())) {
            return false;
        }

        stats.setSkillPoints(progress.playerSkillPoints());
        if (!stats.unlockSkillNode(node.id())) {
            progress.addPlayerSkillPoints(node.cost());
            stats.setSkillPoints(progress.playerSkillPoints());
            return false;
        }

        progress.markSyncDirty();
        ProgressionService.flushDirty(player);
        return true;
    }
}

