package com.extremecraft.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
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
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHandler.apply(packet.success(), packet.message())));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void apply(boolean success, String message) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            Component text = Component.literal((success ? "[OK] " : "[Module] ") + message);
            mc.player.displayClientMessage(text, true);
        }
    }
}
