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
        Action action = msg == null || msg.action == null ? Action.USE_ITEM : msg.action;
        buf.writeEnum(action);
        int entityId = msg == null ? -1 : msg.entityId;
        buf.writeInt(entityId);

        BlockPos pos = msg == null ? null : msg.pos;
        Direction face = msg == null || msg.face == null ? Direction.UP : msg.face;
        buf.writeBoolean(pos != null);
        if (pos != null) {
            buf.writeBlockPos(pos);
            buf.writeEnum(face);
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
            ServerLevel level = sp.serverLevel();

            if (msg == null || msg.action == null) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S with null action from {}", sp.getScoreboardName());
                return;
            }

            if ((msg.action == Action.USE_ON_BLOCK
                    || msg.action == Action.TAP_BREAK
                    || msg.action == Action.HOLD_START_BREAK)
                    && (msg.pos == null || msg.face == null)) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S action={} with missing block context from {}",
                        msg.action, sp.getScoreboardName());
                return;
            }

            if (msg.action == Action.ATTACK_ENTITY && msg.entityId <= 0) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S ATTACK_ENTITY with invalid entityId={} from {}",
                        msg.entityId, sp.getScoreboardName());
                return;
            }

            OffhandActionValidator.ValidationResult validation = OffhandActionValidator.validate(sp, msg);
            if (!validation.accepted()) {
                LOGGER.debug("[Network] Dropped OffhandActionC2S failing validator action={} from {}",
                        msg.action, sp.getScoreboardName());
                return;
            }

            OffhandActionExecutor.execute(sp, level, msg, validation);
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
