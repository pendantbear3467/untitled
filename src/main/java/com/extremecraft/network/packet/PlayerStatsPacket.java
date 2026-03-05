package com.extremecraft.network.packet;

import com.extremecraft.progression.capability.PlayerStatsProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record PlayerStatsPacket(CompoundTag data) {
    public static void encode(PlayerStatsPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static PlayerStatsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new PlayerStatsPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(PlayerStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            mc.player.getCapability(PlayerStatsProvider.PLAYER_STATS)
                    .ifPresent(stats -> stats.deserializeNBT(packet.data()));
        });
        ctx.get().setPacketHandled(true);
    }
}
