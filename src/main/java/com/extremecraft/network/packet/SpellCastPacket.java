package com.extremecraft.network.packet;

import com.extremecraft.config.Config;
import com.extremecraft.magic.SpellCastContext;
import com.extremecraft.magic.SpellExecutor;
import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record SpellCastPacket() {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(SpellCastPacket packet, FriendlyByteBuf buf) {
    }

    public static SpellCastPacket decode(FriendlyByteBuf buf) {
        return new SpellCastPacket();
    }

    public static void handle(SpellCastPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped SpellCastPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped SpellCastPacket due to missing sender or spectator state");
                return;
            }

            if (!Config.areAbilitiesEnabled()) {
                LOGGER.debug("[Network] Dropped SpellCastPacket because abilities are disabled in common config");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "ability.spell", 1, 4, 20)) {
                LOGGER.debug("[Network] Rate-limited SpellCastPacket from {}", sender.getScoreboardName());
                return;
            }

            SpellExecutor.tryCastFromEquipped(sender, SpellCastContext.CastSource.KEYBIND);
        });
        context.setPacketHandled(true);
    }
}
