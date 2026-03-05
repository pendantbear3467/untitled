package com.extremecraft.network.packet;

import com.extremecraft.progression.capability.PlayerProgressProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncProgressPacket(CompoundTag data) {
    public static void encode(SyncProgressPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncProgressPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncProgressPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncProgressPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.player.getCapability(PlayerProgressProvider.PLAYER_PROGRESS)
                    .ifPresent(data -> data.deserializeNBT(packet.data));
        });
        ctx.get().setPacketHandled(true);
    }
}
