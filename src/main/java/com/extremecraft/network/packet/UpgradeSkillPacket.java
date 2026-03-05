package com.extremecraft.network.packet;

import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpgradeSkillPacket(String statId) {
    public static void encode(UpgradeSkillPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.statId(), 32);
    }

    public static UpgradeSkillPacket decode(FriendlyByteBuf buf) {
        return new UpgradeSkillPacket(buf.readUtf(32));
    }

    public static void handle(UpgradeSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if (sender != null) {
                PlayerStatsService.upgradeStat(sender, packet.statId());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
