package com.extremecraft.combat.dualwield;

import com.extremecraft.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class DualWieldService {
    private DualWieldService() {}

    public static void cycleLoadout(ServerPlayer player) {
        PlayerDualWieldApi.get(player).ifPresent(data -> {
            data.ensureInitialized(player);
            data.cycleLoadout(player);
            sync(player, data);
        });
    }

    public static void flushDirty(ServerPlayer player) {
        PlayerDualWieldApi.get(player).ifPresent(data -> {
            data.ensureInitialized(player);
            if (data.consumeDirty()) {
                sync(player, data);
            }
        });
    }

    public static void sync(ServerPlayer player, PlayerDualWieldData data) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncDualWieldDataS2C(data.serializeNBT()));
    }
}

