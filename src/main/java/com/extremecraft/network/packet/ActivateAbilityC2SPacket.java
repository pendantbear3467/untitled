package com.extremecraft.network.packet;

import com.extremecraft.ability.AbilityCastResult;
import com.extremecraft.ability.AbilityEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public record ActivateAbilityC2SPacket(UUID playerUuid, String abilityId, Vec3 targetPosition) {
    public static void encode(ActivateAbilityC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUuid == null ? new UUID(0L, 0L) : packet.playerUuid);
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
        boolean hasTarget = packet.targetPosition != null;
        buf.writeBoolean(hasTarget);
        if (hasTarget) {
            buf.writeDouble(packet.targetPosition.x);
            buf.writeDouble(packet.targetPosition.y);
            buf.writeDouble(packet.targetPosition.z);
        }
    }

    public static ActivateAbilityC2SPacket decode(FriendlyByteBuf buf) {
        UUID playerUuid = buf.readUUID();
        String abilityId = buf.readUtf(128);
        Vec3 targetPosition = null;
        if (buf.readBoolean()) {
            targetPosition = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        return new ActivateAbilityC2SPacket(playerUuid, abilityId, targetPosition);
    }

    public static void handle(ActivateAbilityC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            AbilityCastResult result = AbilityEngine.cast(sender, packet.abilityId, packet.playerUuid, packet.targetPosition);
            if (!result.succeeded()) {
                sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Ability failed: " + result.status().name().toLowerCase()), true);
            }
        });
        context.setPacketHandled(true);
    }
}
