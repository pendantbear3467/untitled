package com.extremecraft.machine.menu;

import com.extremecraft.future.registry.TechMenuTypes;
import net.minecraft.world.entity.player.Inventory;

public class PlayerStatsMenu extends PlayerTabMenu {
    public PlayerStatsMenu(int containerId, Inventory inventory) {
        super(TechMenuTypes.PLAYER_STATS.get(), containerId, inventory);
    }
}
