package com.extremecraft.combat.dualwield;

import com.extremecraft.combat.dualwield.service.OffhandRuntimeService;
import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PlayerDualWieldEvents {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ECConstants.MODID, "player_dual_wield");

    @SubscribeEvent
    public void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            PlayerDualWieldProvider provider = new PlayerDualWieldProvider();
            event.addCapability(ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        event.getOriginal().reviveCaps();
        try {
            PlayerDualWieldApi.get(event.getOriginal()).ifPresent(oldData ->
                    PlayerDualWieldApi.get(event.getEntity()).ifPresent(newData -> newData.copyFrom(oldData))
            );
        } finally {
            event.getOriginal().invalidateCaps();
        }

        if (event.getOriginal() instanceof ServerPlayer originalPlayer) {
            OffhandRuntimeService.clearPlayer(originalPlayer, false);
        }
        if (event.getEntity() instanceof ServerPlayer newPlayer) {
            OffhandRuntimeService.clearPlayer(newPlayer, false);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OffhandRuntimeService.clearPlayer(player, false);
            syncState(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OffhandRuntimeService.clearPlayer(player, false);
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OffhandRuntimeService.clearPlayer(player, false);
            syncState(player);
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            OffhandRuntimeService.clearPlayer(player, false);
            syncState(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Batch sync checks to avoid packet spam each tick.
        int interval = 5;
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) == 0) {
            DualWieldService.flushDirty(player);
        }
    }

    private static void syncState(ServerPlayer player) {
        PlayerDualWieldApi.get(player).ifPresent(data -> {
            data.ensureInitialized(player);
            DualWieldService.sync(player, data);
        });
    }
}
