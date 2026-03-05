package com.extremecraft.net;

import com.extremecraft.ExtremeCraftMod;
import com.extremecraft.combat.dualwield.CycleLoadoutC2S;
import com.extremecraft.combat.dualwield.SyncDualWieldDataS2C;
import com.extremecraft.progression.skilltree.SyncSkillTreeDataS2C;
import com.extremecraft.progression.skilltree.UnlockSkillNodeC2S;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class DwNetwork {
    private DwNetwork() {}

    public static final String PROTOCOL = "1";
    public static SimpleChannel CH;

    public static void init() {
        NetworkRegistry.ChannelBuilder main = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ExtremeCraftMod.MODID, "dual_wield"));
        main.networkProtocolVersion(() -> PROTOCOL);
        main.clientAcceptedVersions(PROTOCOL::equals);
        main.serverAcceptedVersions(PROTOCOL::equals);
        CH = main.simpleChannel();

        CH.messageBuilder(OffhandActionC2S.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OffhandActionC2S::encode)
                .decoder(OffhandActionC2S::decode)
                .consumerMainThread(OffhandActionC2S::handle)
                .add();

            CH.messageBuilder(CycleLoadoutC2S.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CycleLoadoutC2S::encode)
                .decoder(CycleLoadoutC2S::decode)
                .consumerMainThread(CycleLoadoutC2S::handle)
                .add();

            CH.messageBuilder(SyncDualWieldDataS2C.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDualWieldDataS2C::encode)
                .decoder(SyncDualWieldDataS2C::decode)
                .consumerMainThread(SyncDualWieldDataS2C::handle)
                .add();

            CH.messageBuilder(UnlockSkillNodeC2S.class, 3, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UnlockSkillNodeC2S::encode)
                .decoder(UnlockSkillNodeC2S::decode)
                .consumerMainThread(UnlockSkillNodeC2S::handle)
                .add();

            CH.messageBuilder(SyncSkillTreeDataS2C.class, 4, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillTreeDataS2C::encode)
                .decoder(SyncSkillTreeDataS2C::decode)
                .consumerMainThread(SyncSkillTreeDataS2C::handle)
                .add();
    }

    public static void sendToServer(Object msg) { CH.sendToServer(msg); }
}
