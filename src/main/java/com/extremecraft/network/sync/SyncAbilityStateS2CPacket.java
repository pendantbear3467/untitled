package com.extremecraft.network.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncAbilityStateS2CPacket(CompoundTag payload) {
    public static void encode(SyncAbilityStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static SyncAbilityStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncAbilityStateS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncAbilityStateS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            RuntimeSyncClientState.applyAbilities(packet.payload());
        });
        ctx.get().setPacketHandled(true);
    }
}
