package com.extremecraft.progression.level;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLevelCapabilityEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_level");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerLevelProvider provider = new PlayerLevelProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            PlayerLevelApi.get(event.getOriginal()).ifPresent(oldData ->
                    PlayerLevelApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
            );
        } finally {
            event.getOriginal().invalidateCaps();
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LevelService.sync(player);
        }
    }
}
