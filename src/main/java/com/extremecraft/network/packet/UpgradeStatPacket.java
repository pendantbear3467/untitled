package com.extremecraft.network.packet;

import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpgradeStatPacket(String upgradeId) {
    public static void encode(UpgradeStatPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.upgradeId(), 128);
    }

    public static UpgradeStatPacket decode(FriendlyByteBuf buf) {
        return new UpgradeStatPacket(buf.readUtf(128));
    }

    public static void handle(UpgradeStatPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PlayerStatsService.applyUpgradeRequest(sender, packet.upgradeId());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
