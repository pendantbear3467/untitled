package com.extremecraft.network.packet;

import com.extremecraft.magic.SpellCastContext;
import com.extremecraft.magic.SpellExecutor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SpellCastPacket() {
    public static void encode(SpellCastPacket packet, FriendlyByteBuf buf) {
    }

    public static SpellCastPacket decode(FriendlyByteBuf buf) {
        return new SpellCastPacket();
    }

    public static void handle(SpellCastPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            SpellExecutor.tryCastFromEquipped(sender, SpellCastContext.CastSource.KEYBIND);
        });
        context.setPacketHandled(true);
    }
}
