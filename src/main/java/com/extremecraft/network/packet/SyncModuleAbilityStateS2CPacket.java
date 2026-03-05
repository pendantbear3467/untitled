package com.extremecraft.network.packet;

import com.extremecraft.modules.runtime.ModuleAbilityClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncModuleAbilityStateS2CPacket(CompoundTag data) {
    public static void encode(SyncModuleAbilityStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncModuleAbilityStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncModuleAbilityStateS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncModuleAbilityStateS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ModuleAbilityClientState.applySync(packet.data));
        ctx.get().setPacketHandled(true);
    }
}
