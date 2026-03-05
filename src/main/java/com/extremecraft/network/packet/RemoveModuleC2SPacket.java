package com.extremecraft.network.packet;

import com.extremecraft.modules.service.ModuleInstallService;
import com.extremecraft.network.ModNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public record RemoveModuleC2SPacket(String moduleId, String targetSlot) {
    public static void encode(RemoveModuleC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.moduleId, 128);
        buf.writeUtf(packet.targetSlot, 32);
    }

    public static RemoveModuleC2SPacket decode(FriendlyByteBuf buf) {
        return new RemoveModuleC2SPacket(buf.readUtf(128), buf.readUtf(32));
    }

    public static void handle(RemoveModuleC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            ModuleInstallService.TargetSlot slot = ModuleInstallService.TargetSlot.byName(packet.targetSlot);
            ModuleInstallService.Result result = ModuleInstallService.remove(sender, slot, packet.moduleId);
            String message = ModuleInstallService.formatResultMessage(result, false, packet.moduleId, slot);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sender), new SyncModuleActionResultS2CPacket(ModuleInstallService.isSuccess(result), message));
        });
        context.setPacketHandled(true);
    }
}
