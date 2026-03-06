package com.extremecraft.network.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncMachineStateS2CPacket(CompoundTag payload) {
    public static void encode(SyncMachineStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static SyncMachineStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMachineStateS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncMachineStateS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            RuntimeSyncClientState.applyMachineStates(packet.payload());
        });
        ctx.get().setPacketHandled(true);
    }
}
