package com.extremecraft.network.packet;

import com.extremecraft.progression.classsystem.ability.ClassAbilityClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Syncs class ability cooldown state to the client.
 */
public record SyncClassAbilityStateS2CPacket(CompoundTag data) {
    public static void encode(SyncClassAbilityStateS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncClassAbilityStateS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncClassAbilityStateS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncClassAbilityStateS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }

            ClassAbilityClientState.applySync(packet.data);
        });
        context.setPacketHandled(true);
    }
}
