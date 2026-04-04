package com.extremecraft.network.packet;

import com.extremecraft.machine.core.TechMachineBlockEntity;
import com.extremecraft.machine.menu.TechMachineMenu;
import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.reactor.ReactorControlService;
import com.extremecraft.reactor.ReactorIdentity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record ReactorControlC2SPacket(BlockPos pos, byte action, int value) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final byte ACTION_SET_ACTIVE = 0;
    public static final byte ACTION_SCRAM = 1;
    public static final byte ACTION_SET_INSERTION = 2;

    public static void encode(ReactorControlC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos == null ? BlockPos.ZERO : packet.pos);
        buf.writeByte(packet.action);
        buf.writeInt(packet.value);
    }

    public static ReactorControlC2SPacket decode(FriendlyByteBuf buf) {
        return new ReactorControlC2SPacket(buf.readBlockPos(), buf.readByte(), buf.readInt());
    }

    public static void handle(ReactorControlC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }
            if (!ServerPacketLimiter.allow(sender, "reactor.control", 2, 10, 20)) {
                return;
            }
            if (!(sender.level() instanceof ServerLevel serverLevel)) {
                return;
            }
            if (!(sender.containerMenu instanceof TechMachineMenu menu)) {
                return;
            }

            BlockPos targetPos = packet.pos == null ? BlockPos.ZERO : packet.pos;
            if (!targetPos.equals(menu.blockPos())) {
                LOGGER.debug("[Network] Dropped reactor packet from {} due to menu/target mismatch", sender.getScoreboardName());
                return;
            }
            if (!menu.stillValid(sender) || sender.distanceToSqr(targetPos.getX() + 0.5D, targetPos.getY() + 0.5D, targetPos.getZ() + 0.5D) > 64.0D) {
                return;
            }

            if (!(serverLevel.getBlockEntity(targetPos) instanceof TechMachineBlockEntity machine)) {
                return;
            }
            if (!ReactorIdentity.isFirstReleaseReactor(machine.getMachineId())) {
                return;
            }

            if (packet.action != ACTION_SET_ACTIVE
                    && packet.action != ACTION_SCRAM
                    && packet.action != ACTION_SET_INSERTION) {
                LOGGER.debug("[Network] Ignored unknown reactor action {} from {}", packet.action, sender.getScoreboardName());
                return;
            }

            boolean changed = switch (packet.action) {
                case ACTION_SET_ACTIVE -> {
                    if (packet.value != 0 && packet.value != 1) {
                        yield false;
                    }
                    yield ReactorControlService.setActive(serverLevel, targetPos, packet.value > 0);
                }
                case ACTION_SCRAM -> ReactorControlService.scram(serverLevel, targetPos);
                case ACTION_SET_INSERTION -> ReactorControlService.setManualInsertion(serverLevel, targetPos, packet.value);
                default -> false;
            };

            if (changed) {
                machine.setChanged();
            }
        });
        context.setPacketHandled(true);
    }
}
