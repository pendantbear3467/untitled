package com.extremecraft.progression;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class PlayerStatsService {
    private PlayerStatsService() {
    }

    public static boolean upgradeStat(ServerPlayer player, String statId) {
        return PlayerStatsApi.get(player).map(stats -> {
            boolean upgraded = stats.upgrade(statId);
            if (upgraded) {
                sync(player, stats);
            }
            return upgraded;
        }).orElse(false);
    }

    public static void sync(ServerPlayer player) {
        PlayerStatsApi.get(player).ifPresent(stats -> sync(player, stats));
    }

    public static void sync(ServerPlayer player, PlayerStatsCapability stats) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new PlayerStatsPacket(stats.serializeNBT()));
    }
}
