package com.extremecraft.combat.dualwield;

import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record SaveLoadoutC2S(int loadoutIndex) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(SaveLoadoutC2S packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.loadoutIndex);
    }

    public static SaveLoadoutC2S decode(FriendlyByteBuf buf) {
        return new SaveLoadoutC2S(buf.readVarInt());
    }

    public static void handle(SaveLoadoutC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped SaveLoadoutC2S from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator() || !sender.isAlive() || sender.isRemoved()) {
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "dualwield.save", 1, 6, 20)) {
                LOGGER.debug("[Network] Rate-limited SaveLoadoutC2S from {}", sender.getScoreboardName());
                return;
            }

            DualWieldService.saveCurrentToLoadout(sender, packet.loadoutIndex);
        });
        context.setPacketHandled(true);
    }
}