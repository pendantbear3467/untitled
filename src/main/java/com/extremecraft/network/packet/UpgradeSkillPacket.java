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

public record UpgradeSkillPacket(String statId) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(UpgradeSkillPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.statId() == null ? "" : packet.statId(), 32);
    }

    public static UpgradeSkillPacket decode(FriendlyByteBuf buf) {
        return new UpgradeSkillPacket(buf.readUtf(32));
    }

    public static void handle(UpgradeSkillPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped UpgradeSkillPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped UpgradeSkillPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "progression.upgrade", 1, 8, 20)) {
                LOGGER.debug("[Network] Rate-limited UpgradeSkillPacket from {}", sender.getScoreboardName());
                return;
            }

            String statId = packet.statId() == null ? "" : packet.statId().trim();
            if (statId.isEmpty()) {
                LOGGER.debug("[Network] Dropped UpgradeSkillPacket with blank stat id from {}", sender.getScoreboardName());
                return;
            }

            PlayerStatsService.applyUpgradeRequest(sender, statId);
        });
        context.setPacketHandled(true);
    }
}
