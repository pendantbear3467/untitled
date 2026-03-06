package com.extremecraft.progression.level;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncPlayerLevelS2CPacket;
import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LevelService {
    private static final Map<UUID, List<String>> GRANTED_ABILITIES = new LinkedHashMap<>();

    private LevelService() {
    }

    public static int grantXp(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int[] levelUps = new int[]{0};
        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelUps[0] = levelData.grantXp(amount);
            sync(player, levelData);
        });

        PlayerStatsService.addExperience(player, amount);
        return levelUps[0];
    }

    public static void setLevel(ServerPlayer player, int level) {
        if (player == null) {
            return;
        }

        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelData.setLevel(level);
            sync(player, levelData);
        });

        com.extremecraft.progression.capability.PlayerStatsApi.get(player).ifPresent(stats -> {
            stats.setLevel(level);
            PlayerStatsService.sync(player, stats);
        });
    }

    public static void sync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        PlayerLevelApi.get(player).ifPresent(levelData -> sync(player, levelData));
    }

    public static void sync(ServerPlayer player, PlayerLevelCapability levelData) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerLevelS2CPacket(levelData.serializeNBT()));
    }

    public static int xpRequiredForLevel(int level) {
        return PlayerLevelCapability.xpRequired(level);
    }

    public static void grantAbility(ServerPlayer player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return;
        }

        List<String> abilities = GRANTED_ABILITIES.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        String normalized = abilityId.trim().toLowerCase();
        if (!abilities.contains(normalized)) {
            abilities.add(normalized);
        }
    }

    public static String abilityInSlot(ServerPlayer player, int slotIndex) {
        if (player == null) {
            return defaultAbilityForSlot(slotIndex);
        }

        List<String> granted = GRANTED_ABILITIES.get(player.getUUID());
        if (granted != null && slotIndex >= 0 && slotIndex < granted.size()) {
            return granted.get(slotIndex);
        }

        return defaultAbilityForSlot(slotIndex);
    }

    private static String defaultAbilityForSlot(int slotIndex) {
        return switch (slotIndex) {
            case 0 -> "firebolt";
            case 1 -> "blink";
            case 2 -> "arcane_shield";
            case 3 -> "meteor";
            default -> "";
        };
    }
}
