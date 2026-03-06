package com.extremecraft.network.packet;

import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.progression.classsystem.ability.ClassAbilityService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * Client request to activate the selected class ability.
 */
public record ActivateClassAbilityC2SPacket(String abilityId) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(ActivateClassAbilityC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.abilityId == null ? "" : packet.abilityId, 128);
    }

    public static ActivateClassAbilityC2SPacket decode(FriendlyByteBuf buf) {
        return new ActivateClassAbilityC2SPacket(buf.readUtf(128));
    }

    public static void handle(ActivateClassAbilityC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped ActivateClassAbilityC2SPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped ActivateClassAbilityC2SPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "ability.class", 1, 4, 20)) {
                LOGGER.debug("[Network] Rate-limited ActivateClassAbilityC2SPacket from {}", sender.getScoreboardName());
                return;
            }

            String abilityId = packet.abilityId == null ? "" : packet.abilityId.trim();
            if (abilityId.isEmpty()) {
                LOGGER.debug("[Network] Dropped ActivateClassAbilityC2SPacket with blank ability id from {}", sender.getScoreboardName());
                return;
            }

            ClassAbilityService.tryActivate(sender, abilityId);
        });
        context.setPacketHandled(true);
    }
}
