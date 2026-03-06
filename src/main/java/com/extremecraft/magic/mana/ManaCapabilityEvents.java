package com.extremecraft.magic.mana;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ManaCapabilityEvents {
    public static final ResourceLocation ID = new ResourceLocation(ECConstants.MODID, "mana");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new ManaCapabilityProvider());
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        ManaApi.get(event.getOriginal()).ifPresent(oldData ->
                ManaApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
        );
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ManaService.refreshFromPlayerData(player);
            ManaService.sync(player);
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ManaService.sync(player);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (event.player instanceof ServerPlayer player) {
            ManaService.tick(player);
        }
    }
}
