package com.extremecraft.network.sync;

import com.extremecraft.ability.AbilityEngine;
import com.extremecraft.magic.SpellCastingSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class RuntimeSyncEvents {
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeSyncService.syncAll(player);
        }
    }

    @SubscribeEvent
    public void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RuntimeSyncService.syncAll(player);
        }
    }

    @SubscribeEvent
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            RuntimeSyncService.syncAll(event.getPlayer());
            return;
        }

        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            RuntimeSyncService.syncAll(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        SpellCastingSystem.tickChanneling(player);
        AbilityEngine.tickChanneling(player);

        int syncInterval = 40;
        int offset = Math.floorMod(player.getUUID().hashCode(), syncInterval);
        if (((player.tickCount + offset) % syncInterval) == 0) {
            RuntimeSyncService.syncStats(player);
            RuntimeSyncService.syncMachineStates(player);
        }
    }
}
