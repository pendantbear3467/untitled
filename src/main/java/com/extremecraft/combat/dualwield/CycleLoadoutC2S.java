package com.extremecraft.combat.dualwield;

import com.extremecraft.network.security.ServerPacketLimiter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CycleLoadoutC2S() {
    public static void encode(CycleLoadoutC2S packet, FriendlyByteBuf buf) {
    }

    public static CycleLoadoutC2S decode(FriendlyByteBuf buf) {
        return new CycleLoadoutC2S();
    }

    public static void handle(CycleLoadoutC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        if (context.getDirection() != NetworkDirection.PLAY_TO_SERVER) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (!ServerPacketLimiter.allow(sender, "dualwield.cycle", 2, 3, 20)) {
                return;
            }

            DualWieldService.cycleLoadout(sender);
        });
        context.setPacketHandled(true);
    }
}
