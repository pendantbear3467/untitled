package com.extremecraft.network.packet;

import com.extremecraft.network.sync.RuntimeSyncClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AbilitySyncPacket(CompoundTag payload) {
    public static void encode(AbilitySyncPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new AbilitySyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(AbilitySyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) {
                return;
            }
            RuntimeSyncClientState.applyAbilities(packet.payload());
        });
        context.setPacketHandled(true);
    }
}
