package com.extremecraft.network.packet;

import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPlayerStatsPacket {
    public static void encode(RequestPlayerStatsPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestPlayerStatsPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerStatsPacket();
    }

    public static void handle(RequestPlayerStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PlayerStatsService.sync(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
