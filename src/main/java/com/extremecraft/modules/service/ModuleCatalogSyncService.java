package com.extremecraft.modules.service;

import com.extremecraft.modules.data.ModuleDefinition;
import com.extremecraft.modules.registry.ArmorModuleRegistry;
import com.extremecraft.modules.registry.ToolModuleRegistry;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncModuleCatalogS2CPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public final class ModuleCatalogSyncService {
    private ModuleCatalogSyncService() {
    }

    public static void sync(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        root.put("armor", serializeModules(ArmorModuleRegistry.all()));
        root.put("tools", serializeModules(ToolModuleRegistry.all()));

        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncModuleCatalogS2CPacket(root));
    }

    private static ListTag serializeModules(java.util.Collection<ModuleDefinition> modules) {
        ListTag list = new ListTag();
        for (ModuleDefinition module : modules) {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", module.id());
            tag.putInt("slot_cost", module.slotCost());
            tag.putString("required_skill_node", module.requiredSkillNode());
            list.add(tag);
        }
        return list;
    }
}
