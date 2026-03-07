package com.extremecraft.progression.level;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncPlayerLevelS2CPacket;
import com.extremecraft.progression.ProgressionMutationService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class LevelService {
    private LevelService() {
    }

    public static int grantXp(ServerPlayer player, int amount) {
        return ProgressionMutationService.grantXp(player, amount);
    }

    public static int grantLegacyXp(ServerPlayer player, int amount, boolean syncImmediately) {
        if (player == null || amount <= 0) {
            return 0;
        }

        int[] levelUps = new int[]{0};
        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelUps[0] = levelData.grantXp(amount);
            if (syncImmediately) {
                sync(player, levelData);
            }
        });

        return levelUps[0];
    }

    public static void setLevel(ServerPlayer player, int level) {
        ProgressionMutationService.setLevel(player, level);
    }

    public static void setLegacyLevel(ServerPlayer player, int level, boolean syncImmediately) {
        if (player == null) {
            return;
        }

        PlayerLevelApi.get(player).ifPresent(levelData -> {
            levelData.setLevel(level);
            if (syncImmediately) {
                sync(player, levelData);
            }
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
