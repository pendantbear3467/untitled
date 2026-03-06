package com.extremecraft.network.packet;

import com.extremecraft.ability.AbilityEngine;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record AbilityCastPacket(String abilityId) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(AbilityCastPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
    }

    public static AbilityCastPacket decode(FriendlyByteBuf buf) {
        return new AbilityCastPacket(buf.readUtf(128));
    }

    public static void handle(AbilityCastPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped AbilityCastPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped AbilityCastPacket due to missing sender or spectator state");
                return;
            }

            String abilityId = packet.abilityId == null ? "" : packet.abilityId.trim();
            if (abilityId.isEmpty()) {
                LOGGER.debug("[Network] Dropped AbilityCastPacket with blank ability id from {}", sender.getScoreboardName());
                return;
            }

            AbilityEngine.cast(sender, abilityId);
        });
        context.setPacketHandled(true);
    }
}
