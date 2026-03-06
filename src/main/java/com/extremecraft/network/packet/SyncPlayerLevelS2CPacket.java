package com.extremecraft.network.packet;

import com.extremecraft.progression.level.PlayerLevelProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncPlayerLevelS2CPacket(CompoundTag data) {
    public static void encode(SyncPlayerLevelS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncPlayerLevelS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncPlayerLevelS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncPlayerLevelS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            minecraft.player.getCapability(PlayerLevelProvider.PLAYER_LEVEL)
                    .ifPresent(level -> level.deserializeNBT(packet.data));
        });
        context.setPacketHandled(true);
    }
}
