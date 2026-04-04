package com.extremecraft.progression.level;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerLevelEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "player_level");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new PlayerLevelProvider());
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        PlayerLevelApi.get(event.getOriginal()).ifPresent(oldData ->
                PlayerLevelApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
        );
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            LevelService.sync(player);
        }
    }
}
