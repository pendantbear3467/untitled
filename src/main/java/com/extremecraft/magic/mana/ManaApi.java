package com.extremecraft.magic.mana;

import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ManaApi {
    private ManaApi() {
    }

    public static Optional<ManaCapability> get(Player player) {
        return player.getCapability(ManaCapabilityProvider.MANA_CAPABILITY).resolve();
    }
}
