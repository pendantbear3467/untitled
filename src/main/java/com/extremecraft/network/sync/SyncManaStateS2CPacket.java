package com.extremecraft.network.sync;

import com.extremecraft.magic.mana.ManaCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncManaStateS2CPacket(CompoundTag payload) {
    public static void encode(SyncManaStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static SyncManaStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncManaStateS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncManaStateS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            minecraft.player.getCapability(ManaCapabilityProvider.MANA_CAPABILITY)
                    .ifPresent(mana -> mana.deserializeNBT(packet.payload()));
        });
        ctx.get().setPacketHandled(true);
    }
}
