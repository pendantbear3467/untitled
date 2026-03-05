package com.extremecraft.network;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.packet.PlayerStatsPacket;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.SyncProgressPacket;
import com.extremecraft.network.packet.UpgradeSkillPacket;
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

        CHANNEL.messageBuilder(UpgradeSkillPacket.class, nextId())
                .encoder(UpgradeSkillPacket::encode)
                .decoder(UpgradeSkillPacket::decode)
                .consumerMainThread(UpgradeSkillPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestPlayerStatsPacket.class, nextId())
                .encoder(RequestPlayerStatsPacket::encode)
                .decoder(RequestPlayerStatsPacket::decode)
                .consumerMainThread(RequestPlayerStatsPacket::handle)
                .add();
    }

    private static int nextId() {
        return index++;
    }
}
