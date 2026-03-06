package com.extremecraft.net;

import com.extremecraft.combat.dualwield.service.OffhandActionExecutor;
import com.extremecraft.combat.dualwield.validation.OffhandActionValidator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record OffhandActionC2S(Action action, int entityId, BlockPos pos, Direction face) {
    private static final Logger LOGGER = LogManager.getLogger();

    public enum Action {ATTACK_ENTITY, USE_ITEM, USE_ON_BLOCK, TAP_BREAK, HOLD_START_BREAK, HOLD_ABORT_BREAK}

    public static void encode(OffhandActionC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.pos != null);
        if (msg.pos != null) {
            buf.writeBlockPos(msg.pos);
            buf.writeEnum(msg.face);
        }
    }

    public static OffhandActionC2S decode(FriendlyByteBuf buf) {
        Action a = buf.readEnum(Action.class);
        int id = buf.readInt();
        BlockPos p = null;
        Direction f = null;
        if (buf.readBoolean()) {
            p = buf.readBlockPos();
            f = buf.readEnum(Direction.class);
        }
        return new OffhandActionC2S(a, id, p, f);
    }

    public static void handle(OffhandActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped OffhandActionC2S from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sp = context.getSender();
            if (sp == null || sp.isSpectator()) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S due to missing sender or spectator state");
                return;
            }

            if (msg == null || msg.action() == null) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S with missing action from {}", sp.getScoreboardName());
                return;
            }

            ServerLevel level = sp.serverLevel();

            if (!OffhandActionValidator.canHandle(sp, msg)) {
                LOGGER.debug("[Network] Rejected OffhandActionC2S action={} for {}", msg.action(), sp.getScoreboardName());
                return;
            }

            OffhandActionExecutor.execute(sp, level, msg);
        });
        context.setPacketHandled(true);
    }

    public static boolean withinReach(ServerPlayer sp, BlockPos pos) {
        return OffhandActionValidator.withinReach(sp, pos);
    }

    public static void callHandleBlockBreakAction(ServerPlayer sp, BlockPos pos, Direction face,
                                                  ServerboundPlayerActionPacket.Action action) {
        OffhandActionExecutor.callHandleBlockBreakAction(sp, pos, face, action);
    }
}
