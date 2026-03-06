package com.extremecraft.network.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncRuntimeStatsS2CPacket(CompoundTag payload) {
    public static void encode(SyncRuntimeStatsS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static SyncRuntimeStatsS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncRuntimeStatsS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncRuntimeStatsS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            RuntimeSyncClientState.applyStats(packet.payload());
        });
        ctx.get().setPacketHandled(true);
    }
}
