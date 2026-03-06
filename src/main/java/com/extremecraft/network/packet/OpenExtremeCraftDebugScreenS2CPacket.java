package com.extremecraft.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
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
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHandler::openDebugScreen);
        });
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void openDebugScreen() {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            mc.setScreen(new com.extremecraft.client.gui.debug.ExtremeCraftDebugScreen(mc.screen));
        }
    }
}
