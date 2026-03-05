package com.extremecraft.net;

import com.extremecraft.ExtremeCraftMod;
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
                .named(new ResourceLocation(ExtremeCraftMod.MODID, "main"));
        main.networkProtocolVersion(() -> PROTOCOL);
        main.clientAcceptedVersions(PROTOCOL::equals);
        main.serverAcceptedVersions(PROTOCOL::equals);
        CH = main.simpleChannel();

        CH.messageBuilder(OffhandActionC2S.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder(OffhandActionC2S::encode)
                .decoder(OffhandActionC2S::decode)
                .consumerMainThread(OffhandActionC2S::handle)
                .add();
    }

    public static void sendToServer(Object msg) { CH.sendToServer(msg); }
}
