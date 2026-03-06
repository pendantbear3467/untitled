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

public record SyncMaterialsPacket(CompoundTag data) {
    public static void encode(SyncMaterialsPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncMaterialsPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMaterialsPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncMaterialsPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<PlatformDataClientState.MaterialEntry> entries = new ArrayList<>();
            ListTag list = packet.data.getList("materials", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                String id = tag.getString("id");
                String rarity = tag.getString("rarity");
                if (!id.isBlank()) {
                    entries.add(new PlatformDataClientState.MaterialEntry(id, rarity));
                }
            }
            PlatformDataClientState.setMaterials(entries);
        });
        ctx.get().setPacketHandled(true);
    }
}
