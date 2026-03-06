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

public record SyncMachinesPacket(CompoundTag data) {
    public static void encode(SyncMachinesPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncMachinesPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncMachinesPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncMachinesPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<PlatformDataClientState.MachineEntry> entries = new ArrayList<>();
            ListTag list = packet.data.getList("machines", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag tag = list.getCompound(i);
                String id = tag.getString("id");
                String tier = tag.getString("tier");
                int ept = tag.getInt("energy_per_tick");
                if (!id.isBlank()) {
                    entries.add(new PlatformDataClientState.MachineEntry(id, tier, ept));
                }
            }
            PlatformDataClientState.setMachines(entries);
        });
        ctx.get().setPacketHandled(true);
    }
}
