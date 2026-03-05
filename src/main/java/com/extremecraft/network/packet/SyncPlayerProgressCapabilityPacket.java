package com.extremecraft.network.packet;

import com.extremecraft.progression.capability.PlayerProgressCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncPlayerProgressCapabilityPacket(CompoundTag data) {
    public static void encode(SyncPlayerProgressCapabilityPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncPlayerProgressCapabilityPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncPlayerProgressCapabilityPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncPlayerProgressCapabilityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            mc.player.getCapability(PlayerProgressCapabilityProvider.PLAYER_PROGRESS_CAPABILITY)
                    .ifPresent(data -> data.deserializeNBT(packet.data));
        });
        ctx.get().setPacketHandled(true);
    }
}
