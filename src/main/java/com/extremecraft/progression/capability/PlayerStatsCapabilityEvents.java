package com.extremecraft.progression.capability;

import com.extremecraft.core.ECConstants;
import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerStatsCapabilityEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_stats");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new PlayerStatsProvider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerStatsApi.get(event.getOriginal()).ifPresent(oldData ->
                PlayerStatsApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
        );
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PlayerStatsService.sync(player);
        }
    }
}
