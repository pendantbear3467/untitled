package com.extremecraft.network.packet;

import com.extremecraft.ability.AbilityEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AbilityCastPacket(String abilityId) {
    public static void encode(AbilityCastPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
    }

    public static AbilityCastPacket decode(FriendlyByteBuf buf) {
        return new AbilityCastPacket(buf.readUtf(128));
    }

    public static void handle(AbilityCastPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            AbilityEngine.cast(sender, packet.abilityId);
        });
        context.setPacketHandled(true);
    }
}
