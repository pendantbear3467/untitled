package com.extremecraft.network;

import com.extremecraft.combat.dualwield.CycleLoadoutC2S;
import com.extremecraft.combat.dualwield.SaveLoadoutC2S;
import com.extremecraft.combat.dualwield.SelectLoadoutC2S;
import com.extremecraft.combat.dualwield.SyncDualWieldDataS2C;
import com.extremecraft.core.ECConstants;
import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.network.packet.AbilityCastPacket;
import com.extremecraft.network.packet.AbilitySyncPacket;
import com.extremecraft.network.packet.ActivateAbilityC2SPacket;
import com.extremecraft.network.packet.ActivateClassAbilityC2SPacket;
import com.extremecraft.network.packet.InstallModuleC2SPacket;
import com.extremecraft.network.sync.SyncManaStateS2CPacket;
import com.extremecraft.network.packet.OpenExtremeCraftDebugScreenS2CPacket;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.network.packet.RemoveModuleC2SPacket;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.SpellCastPacket;
import com.extremecraft.network.packet.SyncClassAbilityStateS2CPacket;
import com.extremecraft.network.packet.SyncMachinesPacket;
import com.extremecraft.network.packet.SyncMaterialsPacket;
import com.extremecraft.network.packet.SyncModuleAbilityStateS2CPacket;
import com.extremecraft.network.packet.SyncModuleActionResultS2CPacket;
import com.extremecraft.network.packet.SyncModuleCatalogS2CPacket;
import com.extremecraft.network.packet.SyncPlayerLevelS2CPacket;
import com.extremecraft.network.packet.SyncProgressPacket;
import com.extremecraft.network.packet.SyncSkillTreesPacket;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.network.sync.SyncAbilityStateS2CPacket;
import com.extremecraft.network.sync.SyncMachineStateS2CPacket;
import com.extremecraft.network.sync.SyncRuntimeStatsS2CPacket;
import com.extremecraft.network.sync.SyncSkillUnlocksS2CPacket;
import com.extremecraft.progression.skilltree.SyncSkillTreeDataS2C;
import com.extremecraft.progression.skilltree.UnlockSkillNodeC2S;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Canonical packet channel and registration owner for ExtremeCraft.
 * Compatibility wrappers (for example {@code DwNetwork}) must delegate here and never re-register packets.
 */
public final class ModNetwork {
    private static final Logger LOGGER = LogManager.getLogger();
    private static int index = 0;
    private static boolean initialized = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ECConstants.MODID, "main"),
            () -> ECConstants.NETWORK_PROTOCOL,
            ECConstants.NETWORK_PROTOCOL::equals,
            ECConstants.NETWORK_PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            LOGGER.warn("[Network] ModNetwork.init() called more than once; skipping duplicate packet registration");
            return;
        }
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

        CHANNEL.messageBuilder(SyncPlayerLevelS2CPacket.class, nextId())
                .encoder(SyncPlayerLevelS2CPacket::encode)
                .decoder(SyncPlayerLevelS2CPacket::decode)
                .consumerMainThread(SyncPlayerLevelS2CPacket::handle)
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

        CHANNEL.messageBuilder(ActivateAbilityC2SPacket.class, nextId())
                .encoder(ActivateAbilityC2SPacket::encode)
                .decoder(ActivateAbilityC2SPacket::decode)
                .consumerMainThread(ActivateAbilityC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(ActivateClassAbilityC2SPacket.class, nextId())
                .encoder(ActivateClassAbilityC2SPacket::encode)
                .decoder(ActivateClassAbilityC2SPacket::decode)
                .consumerMainThread(ActivateClassAbilityC2SPacket::handle)
                .add();

        CHANNEL.messageBuilder(SpellCastPacket.class, nextId())
                .encoder(SpellCastPacket::encode)
                .decoder(SpellCastPacket::decode)
                .consumerMainThread(SpellCastPacket::handle)
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

        CHANNEL.messageBuilder(SyncManaStateS2CPacket.class, nextId())
                .encoder(SyncManaStateS2CPacket::encode)
                .decoder(SyncManaStateS2CPacket::decode)
                .consumerMainThread(SyncManaStateS2CPacket::handle)
                .add();

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

        CHANNEL.messageBuilder(OffhandActionC2S.class, nextId())
                .encoder(OffhandActionC2S::encode)
                .decoder(OffhandActionC2S::decode)
                .consumerMainThread(OffhandActionC2S::handle)
                .add();

        CHANNEL.messageBuilder(CycleLoadoutC2S.class, nextId())
                .encoder(CycleLoadoutC2S::encode)
                .decoder(CycleLoadoutC2S::decode)
                .consumerMainThread(CycleLoadoutC2S::handle)
                .add();

        CHANNEL.messageBuilder(SelectLoadoutC2S.class, nextId())
                .encoder(SelectLoadoutC2S::encode)
                .decoder(SelectLoadoutC2S::decode)
                .consumerMainThread(SelectLoadoutC2S::handle)
                .add();

        CHANNEL.messageBuilder(SaveLoadoutC2S.class, nextId())
                .encoder(SaveLoadoutC2S::encode)
                .decoder(SaveLoadoutC2S::decode)
                .consumerMainThread(SaveLoadoutC2S::handle)
                .add();

        CHANNEL.messageBuilder(SyncDualWieldDataS2C.class, nextId())
                .encoder(SyncDualWieldDataS2C::encode)
                .decoder(SyncDualWieldDataS2C::decode)
                .consumerMainThread(SyncDualWieldDataS2C::handle)
                .add();

        CHANNEL.messageBuilder(UnlockSkillNodeC2S.class, nextId())
                .encoder(UnlockSkillNodeC2S::encode)
                .decoder(UnlockSkillNodeC2S::decode)
                .consumerMainThread(UnlockSkillNodeC2S::handle)
                .add();

        CHANNEL.messageBuilder(SyncSkillTreeDataS2C.class, nextId())
                .encoder(SyncSkillTreeDataS2C::encode)
                .decoder(SyncSkillTreeDataS2C::decode)
                .consumerMainThread(SyncSkillTreeDataS2C::handle)
                .add();

        initialized = true;
    }

    private static int nextId() {
        return index++;
    }

    public static synchronized boolean isInitialized() {
        return initialized;
    }
}





