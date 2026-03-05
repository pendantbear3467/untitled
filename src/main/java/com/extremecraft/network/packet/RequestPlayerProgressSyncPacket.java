package com.extremecraft.network.packet;

import com.extremecraft.progression.capability.PlayerProgressCapabilityApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPlayerProgressSyncPacket {
    public static void encode(RequestPlayerProgressSyncPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestPlayerProgressSyncPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerProgressSyncPacket();
    }

    public static void handle(RequestPlayerProgressSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PlayerProgressCapabilityApi.sync(sender);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
