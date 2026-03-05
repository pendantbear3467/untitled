package com.extremecraft.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncModuleActionResultS2CPacket(boolean success, String message) {
    public static void encode(SyncModuleActionResultS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.success);
        buf.writeUtf(packet.message, 256);
    }

    public static SyncModuleActionResultS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncModuleActionResultS2CPacket(buf.readBoolean(), buf.readUtf(256));
    }

    public static void handle(SyncModuleActionResultS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            Component text = Component.literal((packet.success ? "[OK] " : "[Module] ") + packet.message);
            mc.player.displayClientMessage(text, true);
        });
        ctx.get().setPacketHandled(true);
    }
}
