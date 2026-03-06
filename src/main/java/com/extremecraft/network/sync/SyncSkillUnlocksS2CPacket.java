package com.extremecraft.network.sync;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSkillUnlocksS2CPacket(CompoundTag payload) {
    public static void encode(SyncSkillUnlocksS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static SyncSkillUnlocksS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncSkillUnlocksS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncSkillUnlocksS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            RuntimeSyncClientState.applySkillUnlocks(packet.payload());
        });
        ctx.get().setPacketHandled(true);
    }
}
