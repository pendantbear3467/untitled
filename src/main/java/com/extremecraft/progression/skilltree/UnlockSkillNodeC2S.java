package com.extremecraft.progression.skilltree;

import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record UnlockSkillNodeC2S(String treeId, String nodeId) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(UnlockSkillNodeC2S packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.treeId, 64);
        buf.writeUtf(packet.nodeId, 128);
    }

    public static UnlockSkillNodeC2S decode(FriendlyByteBuf buf) {
        return new UnlockSkillNodeC2S(buf.readUtf(64), buf.readUtf(128));
    }

    public static void handle(UnlockSkillNodeC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped UnlockSkillNodeC2S from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "skilltree.unlock", 1, 8, 20)) {
                LOGGER.debug("[Network] Rate-limited UnlockSkillNodeC2S from {}", sender.getScoreboardName());
                return;
            }

            String treeId = packet.treeId == null ? "" : packet.treeId.trim();
            String nodeId = packet.nodeId == null ? "" : packet.nodeId.trim();
            if (treeId.isEmpty() || nodeId.isEmpty()) {
                LOGGER.debug("[Network] Dropped UnlockSkillNodeC2S with blank ids from {}", sender.getScoreboardName());
                return;
            }

            SkillTreeService.tryUnlock(sender, treeId, nodeId);
        });
        context.setPacketHandled(true);
    }
}
