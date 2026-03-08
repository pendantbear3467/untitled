package com.extremecraft.combat.dualwield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncDualWieldDataS2C(CompoundTag data) {
    public static void encode(SyncDualWieldDataS2C packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncDualWieldDataS2C decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncDualWieldDataS2C(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncDualWieldDataS2C packet, Supplier<NetworkEvent.Context> ctx) {
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
            mc.player.getCapability(PlayerDualWieldProvider.PLAYER_DUAL_WIELD)
                    .ifPresent(capability -> capability.deserializeNBT(data));
        }
    }
}
