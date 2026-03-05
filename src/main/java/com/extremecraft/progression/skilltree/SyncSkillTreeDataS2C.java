package com.extremecraft.progression.skilltree;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record SyncSkillTreeDataS2C(CompoundTag data) {
    public static void encode(SyncSkillTreeDataS2C packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncSkillTreeDataS2C decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncSkillTreeDataS2C(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncSkillTreeDataS2C packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            mc.player.getCapability(PlayerSkillDataProvider.PLAYER_SKILL_DATA)
                    .ifPresent(data -> data.deserializeNBT(packet.data));
        });
        context.setPacketHandled(true);
    }
}
