package com.extremecraft.combat.dualwield;

import com.extremecraft.network.security.ServerPacketLimiter;
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
            if (sender == null || sender.isSpectator() || !sender.isAlive() || sender.isRemoved()) {
                LOGGER.debug("[Network] Dropped CycleLoadoutC2S due to sender state sender={} spectator={} alive={} removed={}",
                        sender == null ? "null" : sender.getScoreboardName(),
                        sender != null && sender.isSpectator(),
                        sender != null && sender.isAlive(),
                        sender != null && sender.isRemoved());
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "dualwield.cycle", 2, 3, 20)) {
                LOGGER.debug("[Network] Rate-limited CycleLoadoutC2S from {}", sender.getScoreboardName());
                return;
            }

            DualWieldService.cycleLoadout(sender);
        });
        context.setPacketHandled(true);
    }
}
