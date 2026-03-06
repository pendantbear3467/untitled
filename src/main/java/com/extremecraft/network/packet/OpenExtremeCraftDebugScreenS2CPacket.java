package com.extremecraft.network.packet;

import com.extremecraft.client.gui.debug.ExtremeCraftDebugScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenExtremeCraftDebugScreenS2CPacket() {
    public static void encode(OpenExtremeCraftDebugScreenS2CPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenExtremeCraftDebugScreenS2CPacket decode(FriendlyByteBuf buf) {
        return new OpenExtremeCraftDebugScreenS2CPacket();
    }

    public static void handle(OpenExtremeCraftDebugScreenS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new ExtremeCraftDebugScreen(mc.screen));
        });
        ctx.get().setPacketHandled(true);
    }
}
