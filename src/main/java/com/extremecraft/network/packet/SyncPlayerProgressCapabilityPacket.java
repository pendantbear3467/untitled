package com.extremecraft.network.packet;

import com.extremecraft.progression.capability.PlayerProgressCapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncPlayerProgressCapabilityPacket(CompoundTag data) {
    public static void encode(SyncPlayerProgressCapabilityPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncPlayerProgressCapabilityPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncPlayerProgressCapabilityPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncPlayerProgressCapabilityPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHandler.apply(packet.data())));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void apply(CompoundTag data) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            mc.player.getCapability(PlayerProgressCapabilityProvider.PLAYER_PROGRESS_CAPABILITY)
                    .ifPresent(capability -> capability.deserializeNBT(data));
        }
    }
}
