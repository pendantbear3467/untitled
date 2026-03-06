package com.extremecraft.network.packet;

import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.progression.capability.PlayerProgressCapabilityApi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPlayerProgressSyncPacket {
    public static void encode(RequestPlayerProgressSyncPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestPlayerProgressSyncPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerProgressSyncPacket();
    }

    public static void handle(RequestPlayerProgressSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "sync.request.progress", 5, 4, 40)) {
                return;
            }

            PlayerProgressCapabilityApi.sync(sender);
        });
        context.setPacketHandled(true);
    }
}
