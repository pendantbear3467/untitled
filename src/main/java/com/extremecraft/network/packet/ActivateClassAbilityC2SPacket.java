package com.extremecraft.network.packet;

import com.extremecraft.progression.classsystem.ability.ClassAbilityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client request to activate the selected class ability.
 */
public record ActivateClassAbilityC2SPacket(String abilityId) {
    public static void encode(ActivateClassAbilityC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
    }

    public static ActivateClassAbilityC2SPacket decode(FriendlyByteBuf buf) {
        return new ActivateClassAbilityC2SPacket(buf.readUtf(128));
    }

    public static void handle(ActivateClassAbilityC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            ClassAbilityService.tryActivate(sender, packet.abilityId);
        });
        context.setPacketHandled(true);
    }
}
