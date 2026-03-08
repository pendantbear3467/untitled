package com.extremecraft.network.packet;

import com.extremecraft.network.sync.RuntimeSyncClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AbilitySyncPacket(CompoundTag payload) {
    public static void encode(AbilitySyncPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static AbilitySyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new AbilitySyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(AbilitySyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHandler.apply(packet.payload())));
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientHandler {
        private ClientHandler() {
        }

        private static void apply(CompoundTag payload) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            RuntimeSyncClientState.applyAbilities(payload);
        }
    }
}
