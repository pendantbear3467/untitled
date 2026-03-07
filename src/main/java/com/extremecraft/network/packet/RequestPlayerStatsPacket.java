package com.extremecraft.network.packet;

import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class RequestPlayerStatsPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(RequestPlayerStatsPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestPlayerStatsPacket decode(FriendlyByteBuf buf) {
        return new RequestPlayerStatsPacket();
    }

    public static void handle(RequestPlayerStatsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped RequestPlayerStatsPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped RequestPlayerStatsPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "sync.request.stats", 5, 4, 40)) {
                LOGGER.debug("[Network] Rate-limited RequestPlayerStatsPacket from {}", sender.getScoreboardName());
                return;
            }

            PlayerStatsService.sync(sender);
        });
        context.setPacketHandled(true);
    }
}
