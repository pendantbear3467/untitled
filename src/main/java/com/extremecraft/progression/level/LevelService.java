package com.extremecraft.progression.level;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncPlayerLevelS2CPacket;
import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.PlayerStatsApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class LevelService {
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

        PlayerStatsApi.get(player).ifPresent(stats -> {
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
}
