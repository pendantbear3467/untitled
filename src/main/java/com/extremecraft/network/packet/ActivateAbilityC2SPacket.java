package com.extremecraft.network.packet;

import com.extremecraft.ability.AbilityEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ActivateAbilityC2SPacket(String abilityId, int slotIndex, float yaw, float pitch) {
    public static void encode(ActivateAbilityC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
        buf.writeInt(packet.slotIndex);
        buf.writeFloat(packet.yaw);
        buf.writeFloat(packet.pitch);
    }

    public static ActivateAbilityC2SPacket decode(FriendlyByteBuf buf) {
        return new ActivateAbilityC2SPacket(buf.readUtf(128), buf.readInt(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(ActivateAbilityC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            AbilityEngine.cast(sender, packet.abilityId, packet.slotIndex);
        });
        context.setPacketHandled(true);
    }
}
