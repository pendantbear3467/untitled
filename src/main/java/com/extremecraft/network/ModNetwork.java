package com.extremecraft.network;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.packet.SyncProgressPacket;
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

    private ModNetwork() {}

    public static void init() {
        CHANNEL.messageBuilder(SyncProgressPacket.class, nextId())
                .encoder(SyncProgressPacket::encode)
                .decoder(SyncProgressPacket::decode)
                .consumerMainThread(SyncProgressPacket::handle)
                .add();
    }

    private static int nextId() {
        return index++;
    }
}
