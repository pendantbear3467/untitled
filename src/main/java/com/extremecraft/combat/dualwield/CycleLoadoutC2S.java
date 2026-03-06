package com.extremecraft.combat.dualwield;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record CycleLoadoutC2S() {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(CycleLoadoutC2S packet, FriendlyByteBuf buf) {
    }

    public static CycleLoadoutC2S decode(FriendlyByteBuf buf) {
        return new CycleLoadoutC2S();
    }

    public static void handle(CycleLoadoutC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped CycleLoadoutC2S from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped CycleLoadoutC2S due to missing sender or spectator state");
                return;
            }
            DualWieldService.cycleLoadout(sender);
        });
        context.setPacketHandled(true);
    }
}
