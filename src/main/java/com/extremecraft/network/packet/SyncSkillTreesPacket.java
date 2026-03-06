package com.extremecraft.network.packet;

import com.extremecraft.platform.data.sync.client.PlatformDataClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncSkillTreesPacket(CompoundTag data) {
    public static void encode(SyncSkillTreesPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncSkillTreesPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncSkillTreesPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncSkillTreesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<PlatformDataClientState.SkillTreeEntry> entries = new ArrayList<>();
            ListTag list = packet.data.getList("skill_trees", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                String id = tag.getString("id");
                int nodes = tag.getInt("nodes");
                if (!id.isBlank()) {
                    entries.add(new PlatformDataClientState.SkillTreeEntry(id, nodes));
                }
            }
            PlatformDataClientState.setSkillTrees(entries);
        });
        ctx.get().setPacketHandled(true);
    }
}
