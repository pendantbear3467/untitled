package com.extremecraft.network.packet;

import com.extremecraft.ability.AbilityCastResult;
import com.extremecraft.ability.AbilityEngine;
import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;
import java.util.function.Supplier;

public record ActivateAbilityC2SPacket(UUID playerUuid, String abilityId, Vec3 targetPosition) {
    private static final Logger LOGGER = LogManager.getLogger();

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
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped ActivateAbilityC2SPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped ActivateAbilityC2SPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "ability.cast", 1, 6, 20)) {
                LOGGER.debug("[Network] Rate-limited ActivateAbilityC2SPacket from {}", sender.getScoreboardName());
                return;
            }

            String abilityId = packet.abilityId == null ? "" : packet.abilityId.trim();
            if (abilityId.isEmpty()) {
                LOGGER.debug("[Network] Dropped ActivateAbilityC2SPacket with blank ability id from {}", sender.getScoreboardName());
                return;
            }

            UUID claimedUuid = packet.playerUuid == null ? new UUID(0L, 0L) : packet.playerUuid;
            if (!claimedUuid.equals(new UUID(0L, 0L)) && !sender.getUUID().equals(claimedUuid)) {
                LOGGER.warn("[Network] Rejected ActivateAbilityC2SPacket uuid mismatch for {}", sender.getScoreboardName());
                return;
            }

            if (packet.targetPosition != null &&
                    (!Double.isFinite(packet.targetPosition.x) || !Double.isFinite(packet.targetPosition.y) || !Double.isFinite(packet.targetPosition.z))) {
                LOGGER.warn("[Network] Rejected ActivateAbilityC2SPacket with non-finite target from {}", sender.getScoreboardName());
                return;
            }

            if (packet.targetPosition != null && sender.position().distanceToSqr(packet.targetPosition) > 4096.0D) {
                LOGGER.warn("[Network] Rejected ActivateAbilityC2SPacket with out-of-range target from {}", sender.getScoreboardName());
                return;
            }

            AbilityCastResult result = AbilityEngine.cast(sender, abilityId, sender.getUUID(), packet.targetPosition);
            if (!result.succeeded()) {
                sender.displayClientMessage(net.minecraft.network.chat.Component.literal("Ability failed: " + result.status().name().toLowerCase()), true);
                LOGGER.debug("[Ability] Activation failed for {} ability={} status={} reason={}",
                    sender.getScoreboardName(), abilityId, result.status(), result.message());
            }
        });
        context.setPacketHandled(true);
    }
}
