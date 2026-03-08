package com.extremecraft.combat.dualwield.service;

import com.extremecraft.combat.dualwield.validation.OffhandActionValidator;
import com.extremecraft.server.DwServerTicker;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Clears runtime-only offhand state that should never outlive the current player session or world context.
 */
public final class OffhandRuntimeService {
    private OffhandRuntimeService() {
    }

    public static void clearPlayer(ServerPlayer player, boolean sendAbort) {
        if (player == null) {
            return;
        }

        DwServerTicker.clearPlayer(player, sendAbort);
        OffhandActionValidator.clearPlayer(player);
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }

        DwServerTicker.clearPlayer(playerId);
        OffhandActionValidator.clearPlayer(playerId);
    }
}