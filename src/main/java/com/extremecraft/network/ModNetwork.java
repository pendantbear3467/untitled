package com.extremecraft.network;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.packet.AbilityCastPacket;
import com.extremecraft.network.packet.AbilitySyncPacket;
import com.extremecraft.network.packet.ActivateClassAbilityC2SPacket;
import com.extremecraft.network.packet.InstallModuleC2SPacket;
import com.extremecraft.network.packet.OpenExtremeCraftDebugScreenS2CPacket;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.network.packet.RemoveModuleC2SPacket;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.SpellCastPacket;`r`nimport com.extremecraft.network.packet.SyncClassAbilityStateS2CPacket;
import com.extremecraft.network.packet.SyncMachinesPacket;
import com.extremecraft.network.packet.SyncMaterialsPacket;
import com.extremecraft.network.packet.SyncModuleAbilityStateS2CPacket;
import com.extremecraft.network.packet.SyncModuleActionResultS2CPacket;
import com.extremecraft.network.packet.SyncModuleCatalogS2CPacket;
import com.extremecraft.network.packet.SyncProgressPacket;
import com.extremecraft.network.packet.SyncSkillTreesPacket;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.network.sync.SyncAbilityStateS2CPacket;
import com.extremecraft.network.sync.SyncMachineStateS2CPacket;
import com.extremecraft.network.packet.ManaSyncPacket;
import com.extremecraft.network.sync.SyncRuntimeStatsS2CPacket;
import com.extremecraft.network.sync.SyncSkillUnlocksS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static int index = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ECConstants.MODID, "main"),
            () -> ECConstants.NETWORK_PROTOCOL,
            ECConstants.NETWORK_PROTOCOL::equals,
            ECConstants.NETWORK_PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static void init() {
        index = 0;

        CHANNEL.messageBuilder(SyncProgressPacket.class, nextId())
                .encoder(SyncProgressPacket::encode)
                .decoder(SyncProgressPacket::decode)
                .consumerMainThread(SyncProgressPacket::handle)
                .add();

        CHANNEL.messageBuilder(PlayerStatsPacket.class, nextId())
                .encoder(PlayerStatsPacket::encode)
                .decoder(PlayerStatsPacket::decode)
                .consumerMainThread(PlayerStatsPacket::handle)
                .add();

        CHANNEL.messageBuilder(UpgradeStatPacket.class, nextId())
                .encoder(UpgradeStatPacket::encode)
                .decoder(UpgradeStatPacket::decode)
                .consumerMainThread(UpgradeStatPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestPlayerStatsPacket.class, nextId())
                .encoder(RequestPlayerStatsPacket::encode)
                .decoder(RequestPlayerStatsPacket::decode)
                .consumerMainThread(RequestPlayerStatsPacket::handle)
                .add();

        CHANNEL.messageBuilder(AbilityCastPacket.class, nextId())
                .encoder(AbilityCastPacket::encode)
                .decoder(AbilityCastPacket::decode)
                .consumerMainThread(AbilityCastPacket::handle)
                .add();

        CHANNEL.messageBuilder(ActivateClassAbilityC2SPacket.class, nextId())
                .encoder(ActivateClassAbilityC2SPacket::encode)
                .decoder(ActivateClassAbilityC2SPacket::decode)
                .consumerMainThread(ActivateClassAbilityC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncClassAbilityStateS2CPacket.class, nextId())
                .encoder(SyncClassAbilityStateS2CPacket::encode)
                .decoder(SyncClassAbilityStateS2CPacket::decode)
                .consumerMainThread(SyncClassAbilityStateS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncModuleAbilityStateS2CPacket.class, nextId())
                .encoder(SyncModuleAbilityStateS2CPacket::encode)
                .decoder(SyncModuleAbilityStateS2CPacket::decode)
                .consumerMainThread(SyncModuleAbilityStateS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncModuleCatalogS2CPacket.class, nextId())
                .encoder(SyncModuleCatalogS2CPacket::encode)
                .decoder(SyncModuleCatalogS2CPacket::decode)
                .consumerMainThread(SyncModuleCatalogS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncModuleActionResultS2CPacket.class, nextId())
                .encoder(SyncModuleActionResultS2CPacket::encode)
                .decoder(SyncModuleActionResultS2CPacket::decode)
                .consumerMainThread(SyncModuleActionResultS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(InstallModuleC2SPacket.class, nextId())
                .encoder(InstallModuleC2SPacket::encode)
                .decoder(InstallModuleC2SPacket::decode)
                .consumerMainThread(InstallModuleC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(RemoveModuleC2SPacket.class, nextId())
                .encoder(RemoveModuleC2SPacket::encode)
                .decoder(RemoveModuleC2SPacket::decode)
                .consumerMainThread(RemoveModuleC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncMachinesPacket.class, nextId())
                .encoder(SyncMachinesPacket::encode)
                .decoder(SyncMachinesPacket::decode)
                .consumerMainThread(SyncMachinesPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncSkillTreesPacket.class, nextId())
                .encoder(SyncSkillTreesPacket::encode)
                .decoder(SyncSkillTreesPacket::decode)
                .consumerMainThread(SyncSkillTreesPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncMaterialsPacket.class, nextId())
                .encoder(SyncMaterialsPacket::encode)
                .decoder(SyncMaterialsPacket::decode)
                .consumerMainThread(SyncMaterialsPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenExtremeCraftDebugScreenS2CPacket.class, nextId())
                .encoder(OpenExtremeCraftDebugScreenS2CPacket::encode)
                .decoder(OpenExtremeCraftDebugScreenS2CPacket::decode)
                .consumerMainThread(OpenExtremeCraftDebugScreenS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(ManaSyncPacket.class, nextId())`r`n                .encoder(ManaSyncPacket::encode)`r`n                .decoder(ManaSyncPacket::decode)`r`n                .consumerMainThread(ManaSyncPacket::handle)`r`n                .add();

        CHANNEL.messageBuilder(SyncRuntimeStatsS2CPacket.class, nextId())
                .encoder(SyncRuntimeStatsS2CPacket::encode)
                .decoder(SyncRuntimeStatsS2CPacket::decode)
                .consumerMainThread(SyncRuntimeStatsS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(AbilitySyncPacket.class, nextId())
                .encoder(AbilitySyncPacket::encode)
                .decoder(AbilitySyncPacket::decode)
                .consumerMainThread(AbilitySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncAbilityStateS2CPacket.class, nextId())
                .encoder(SyncAbilityStateS2CPacket::encode)
                .decoder(SyncAbilityStateS2CPacket::decode)
                .consumerMainThread(SyncAbilityStateS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncSkillUnlocksS2CPacket.class, nextId())
                .encoder(SyncSkillUnlocksS2CPacket::encode)
                .decoder(SyncSkillUnlocksS2CPacket::decode)
                .consumerMainThread(SyncSkillUnlocksS2CPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncMachineStateS2CPacket.class, nextId())
                .encoder(SyncMachineStateS2CPacket::encode)
                .decoder(SyncMachineStateS2CPacket::decode)
                .consumerMainThread(SyncMachineStateS2CPacket::handle)
                .add();
    }

    private static int nextId() {
        return index++;
    }
}

