package com.extremecraft.progression;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PlayerStatsService {
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
                changed = unlockSkillNode(stats, nodeId);
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
        return PlayerStatsApi.get(player).map(stats -> {
            boolean leveled = stats.addExperience(amount);
            sync(player, stats);
            return leveled;
        }).orElse(false);
    }

    public static void tickResources(ServerPlayer player) {
        PlayerStatsApi.get(player).ifPresent(stats -> {
            stats.regenerateResources();
            if ((player.tickCount % 20) == 0) {
                sync(player, stats);
            }
        });
    }

    public static void sync(ServerPlayer player) {
        PlayerStatsApi.get(player).ifPresent(stats -> sync(player, stats));
    }

    public static void sync(ServerPlayer player, PlayerStatsCapability stats) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerStatsPacket(stats.serializeNBT()));
    }

    private static boolean unlockSkillNode(PlayerStatsCapability stats, String nodeId) {
        SkillNode node = SkillTreeManager.getNode(nodeId);
        if (node == null || stats.isSkillUnlocked(node.id())) {
            return false;
        }

        if (stats.level() < node.requiredLevel()) {
            return false;
        }

        for (String req : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(req)) {
                return false;
            }
        }

        return stats.unlockSkillNode(node.id(), node.cost());
    }
}
