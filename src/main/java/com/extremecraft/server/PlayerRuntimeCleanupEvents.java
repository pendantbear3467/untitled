package com.extremecraft.server;

import com.extremecraft.ability.AbilityCooldownManager;
import com.extremecraft.ability.AbilityEngine;
import com.extremecraft.combat.dualwield.validation.OffhandActionValidator;
import com.extremecraft.magic.SpellExecutor;
import com.extremecraft.modules.runtime.ModuleRuntimeService;
import com.extremecraft.network.security.ServerPacketLimiter;
import com.extremecraft.progression.classsystem.ability.ClassAbilityService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Clears runtime-only player state on disconnect to prevent memory growth on long-running servers.
 */
public class PlayerRuntimeCleanupEvents {
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        AbilityEngine.clearPlayer(player);
        AbilityCooldownManager.clearPlayer(player);
        ClassAbilityService.clearPlayer(player);
        ModuleRuntimeService.clearPlayer(player);
        SpellExecutor.clearPlayer(player);
        ServerPacketLimiter.clearPlayer(player);
        OffhandActionValidator.clearPlayer(player);
        DwServerTicker.abortOffhandBreak(player, false);
    }
}
