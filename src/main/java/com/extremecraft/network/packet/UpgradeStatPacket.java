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

public record UpgradeStatPacket(String upgradeId) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(UpgradeStatPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.upgradeId() == null ? "" : packet.upgradeId(), 128);
    }

    public static UpgradeStatPacket decode(FriendlyByteBuf buf) {
        return new UpgradeStatPacket(buf.readUtf(128));
    }

    public static void handle(UpgradeStatPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped UpgradeStatPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped UpgradeStatPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "progression.upgrade", 1, 8, 20)) {
                LOGGER.debug("[Network] Rate-limited UpgradeStatPacket from {}", sender.getScoreboardName());
                return;
            }

            String upgradeId = packet.upgradeId() == null ? "" : packet.upgradeId().trim();
            if (upgradeId.isEmpty()) {
                LOGGER.debug("[Network] Dropped UpgradeStatPacket with blank upgrade id from {}", sender.getScoreboardName());
                return;
            }

            PlayerStatsService.applyUpgradeRequest(sender, upgradeId);
        });
        context.setPacketHandled(true);
    }
}
