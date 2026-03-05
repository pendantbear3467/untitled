package com.extremecraft.progression.skilltree;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UnlockSkillNodeC2S(String treeId, String nodeId) {
    public static void encode(UnlockSkillNodeC2S packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.treeId, 64);
        buf.writeUtf(packet.nodeId, 128);
    }

    public static UnlockSkillNodeC2S decode(FriendlyByteBuf buf) {
        return new UnlockSkillNodeC2S(buf.readUtf(64), buf.readUtf(128));
    }

    public static void handle(UnlockSkillNodeC2S packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.isSpectator()) {
                return;
            }

            if (packet.treeId == null || packet.treeId.isBlank() || packet.nodeId == null || packet.nodeId.isBlank()) {
                return;
            }

            SkillTreeService.tryUnlock(sender, packet.treeId, packet.nodeId);
        });
        context.setPacketHandled(true);
    }
}
