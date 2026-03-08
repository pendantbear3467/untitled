package com.extremecraft.network.packet;

import com.extremecraft.network.sync.SyncManaStateS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Compatibility wrapper retained for legacy call sites.
 * Canonical mana sync ownership lives in {@link SyncManaStateS2CPacket}.
 */
@Deprecated(forRemoval = false)
public record ManaSyncPacket(CompoundTag payload) {
    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buf) {
        CompoundTag safePayload = packet == null || packet.payload == null ? new CompoundTag() : packet.payload;
        SyncManaStateS2CPacket.encode(new SyncManaStateS2CPacket(safePayload), buf);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        SyncManaStateS2CPacket packet = SyncManaStateS2CPacket.decode(buf);
        return new ManaSyncPacket(packet.payload());
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        CompoundTag safePayload = packet == null || packet.payload == null ? new CompoundTag() : packet.payload;
        SyncManaStateS2CPacket.handle(new SyncManaStateS2CPacket(safePayload), ctx);
    }
}
