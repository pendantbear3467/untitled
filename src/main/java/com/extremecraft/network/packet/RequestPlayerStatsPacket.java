package com.extremecraft.network.packet;

import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestPlayerStatsPacket {
    public static void encode(RequestPlayerStatsPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestPlayerStatsPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerStatsPacket();
    }

    public static void handle(RequestPlayerStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
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

            if (!ServerPacketLimiter.allow(sender, "sync.request.stats", 5, 4, 40)) {
                return;
            }

            PlayerStatsService.sync(sender);
        });
        context.setPacketHandled(true);
    }
}
