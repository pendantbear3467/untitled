package com.extremecraft.progression.level;

import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;

public final class LevelService {
    private LevelService() {
    }

    public static boolean grantXp(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return false;
        }

        return PlayerStatsApi.get(player).map(stats -> {
            int oldLevel = stats.level();
            stats.addExperience(amount);
            syncFromStats(player, stats);
            PlayerStatsService.sync(player, stats);
            RuntimeSyncService.syncAbilities(player);
            return stats.level() > oldLevel;
        }).orElse(false);
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (player == null) {
            return;
        }

        PlayerStatsApi.get(player).ifPresent(stats -> {
            stats.setLevel(level);
            syncFromStats(player, stats);
            PlayerStatsService.sync(player, stats);
            RuntimeSyncService.syncAbilities(player);
        });
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }

        PlayerStatsApi.get(player).ifPresent(stats -> {
            syncFromStats(player, stats);
            PlayerStatsService.sync(player, stats);
            RuntimeSyncService.syncAbilities(player);
        });
    }

    public static void grantAbility(ServerPlayer player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return;
        }

        PlayerLevelApi.get(player).ifPresent(data -> {
            if (data.grantAbility(abilityId)) {
                RuntimeSyncService.syncAbilities(player);
            }
        });
    }

    public static String abilityInSlot(ServerPlayer player, int slotIndex) {
        return PlayerLevelApi.get(player)
                .map(data -> data.abilityInSlot(slotIndex))
                .filter(id -> !id.isBlank())
                .orElseGet(() -> defaultAbilityForSlot(slotIndex));
    }

    public static int skillPoints(ServerPlayer player) {
        return PlayerLevelApi.get(player).map(PlayerLevelCapability::skillPoints).orElse(0);
    }

    private static void syncFromStats(ServerPlayer player, PlayerStatsCapability stats) {
        PlayerLevelApi.get(player).ifPresent(levels -> levels.setProgression(stats.level(), stats.experience(), stats.skillPoints()));
    }

    public static String defaultAbilityForSlot(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> "firebolt";
            case 1 -> "blink";
            case 2 -> "arcane_shield";
            case 3 -> "meteor";
            default -> "";
        };
    }
}
