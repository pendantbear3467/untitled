package com.extremecraft.network.packet;

import com.extremecraft.modules.runtime.ModuleCatalogClientState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncModuleCatalogS2CPacket(CompoundTag data) {
    public static void encode(SyncModuleCatalogS2CPacket packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.data);
    }

    public static SyncModuleCatalogS2CPacket decode(FriendlyByteBuf buf) {
        CompoundTag tag = buf.readNbt();
        return new SyncModuleCatalogS2CPacket(tag == null ? new CompoundTag() : tag);
    }

    public static void handle(SyncModuleCatalogS2CPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            List<ModuleCatalogClientState.ModuleEntry> armor = readEntries(packet.data.getList("armor", Tag.TAG_COMPOUND));
            List<ModuleCatalogClientState.ModuleEntry> tools = readEntries(packet.data.getList("tools", Tag.TAG_COMPOUND));
            ModuleCatalogClientState.apply(armor, tools);
        });
        ctx.get().setPacketHandled(true);
    }

    private static List<ModuleCatalogClientState.ModuleEntry> readEntries(ListTag list) {
        List<ModuleCatalogClientState.ModuleEntry> entries = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag tag = list.getCompound(i);
            String id = tag.getString("id");
            int slotCost = Math.max(1, tag.getInt("slot_cost"));
            String requiredSkill = tag.getString("required_skill_node");
            if (!id.isBlank()) {
                entries.add(new ModuleCatalogClientState.ModuleEntry(id, slotCost, requiredSkill));
            }
        }
        return entries;
    }
}
