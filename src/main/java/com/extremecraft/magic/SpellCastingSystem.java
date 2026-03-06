package com.extremecraft.magic;

import com.extremecraft.magic.SpellCastContext.CastSource;
import net.minecraft.server.level.ServerPlayer;

public final class SpellCastingSystem {
    private SpellCastingSystem() {
    }

    public static boolean tryCast(ServerPlayer player, String spellId) {
        return SpellExecutor.tryCast(player, spellId, CastSource.COMMAND);
    }

    public static void tickChanneling(ServerPlayer player) {
        SpellExecutor.tickChanneling(player);
    }
}
