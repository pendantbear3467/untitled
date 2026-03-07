package com.extremecraft.network.packet;

import com.extremecraft.modules.service.ModuleInstallService;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public record RemoveModuleC2SPacket(String moduleId, String targetSlot) {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void encode(RemoveModuleC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.moduleId == null ? "" : packet.moduleId, 128);
        buf.writeUtf(packet.targetSlot == null ? "" : packet.targetSlot, 32);
    }

    public static RemoveModuleC2SPacket decode(FriendlyByteBuf buf) {
        return new RemoveModuleC2SPacket(buf.readUtf(128), buf.readUtf(32));
    }

    public static void handle(RemoveModuleC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            LOGGER.debug("[Network] Dropped RemoveModuleC2SPacket from invalid direction {}", context.getDirection());
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                LOGGER.debug("[Network] Dropped RemoveModuleC2SPacket due to missing sender or spectator state");
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "module.remove", 1, 4, 20)) {
                LOGGER.debug("[Network] Rate-limited RemoveModuleC2SPacket from {}", sender.getScoreboardName());
                return;
            }

            String moduleId = packet.moduleId == null ? "" : packet.moduleId.trim().toLowerCase();
            String requestedSlot = packet.targetSlot == null ? "" : packet.targetSlot.trim();
            if (moduleId.isEmpty()) {
                LOGGER.debug("[Network] Dropped RemoveModuleC2SPacket with blank module id from {}", sender.getScoreboardName());
                return;
            }

            ModuleInstallService.TargetSlot slot = ModuleInstallService.TargetSlot.byName(requestedSlot);
            ModuleInstallService.Result result = ModuleInstallService.remove(sender, slot, moduleId);
            dispatchResult(sender, result, moduleId, slot);
        });
        context.setPacketHandled(true);
    }

    private static void dispatchResult(ServerPlayer sender,
                                       ModuleInstallService.Result result,
                                       String moduleId,
                                       ModuleInstallService.TargetSlot slot) {
        String message = ModuleInstallService.formatResultMessage(result, false, moduleId, slot);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender),
                new SyncModuleActionResultS2CPacket(ModuleInstallService.isSuccess(result), message));
    }
}

