package com.extremecraft.network.packet;

import com.extremecraft.magic.mana.ManaCapabilityProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ManaSyncPacket(CompoundTag payload) {
    public static void encode(ManaSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.payload);
    }

    public static ManaSyncPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new ManaSyncPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(ManaSyncPacket packet, Supplier<NetworkEvent.Context> ctx) {
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
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft.player == null) {
                return;
            }

            minecraft.player.getCapability(ManaCapabilityProvider.MANA_CAPABILITY)
                    .ifPresent(mana -> mana.deserializeNBT(payload));
        }
    }
}
