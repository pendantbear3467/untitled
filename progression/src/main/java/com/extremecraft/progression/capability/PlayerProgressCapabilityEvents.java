package com.extremecraft.progression.capability;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncPlayerProgressCapabilityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class PlayerProgressCapabilityEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_progress_capability");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerProgressCapabilityProvider provider = new PlayerProgressCapabilityProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            PlayerProgressCapabilityApi.get(event.getOriginal()).ifPresent(oldData ->
                    PlayerProgressCapabilityApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
            );
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerProgressCapabilityApi.sync(player);
        }
    }

    public static void sync(ServerPlayer player, PlayerProgressCapability data) {
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncPlayerProgressCapabilityPacket(data.serializeNBT()));
    }
}
