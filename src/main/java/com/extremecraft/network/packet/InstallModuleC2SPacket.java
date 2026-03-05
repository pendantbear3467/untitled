package com.extremecraft.network.packet;

import com.extremecraft.modules.service.ModuleInstallService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InstallModuleC2SPacket(String moduleId, String targetSlot) {
    public static void encode(InstallModuleC2SPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.moduleId, 128);
        buf.writeUtf(packet.targetSlot, 32);
    }

    public static InstallModuleC2SPacket decode(FriendlyByteBuf buf) {
        return new InstallModuleC2SPacket(buf.readUtf(128), buf.readUtf(32));
    }

    public static void handle(InstallModuleC2SPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            ModuleInstallService.install(sender, ModuleInstallService.TargetSlot.byName(packet.targetSlot), packet.moduleId);
        });
        context.setPacketHandled(true);
    }
}
