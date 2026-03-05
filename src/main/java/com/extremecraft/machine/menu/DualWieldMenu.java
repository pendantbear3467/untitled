package com.extremecraft.machine.menu;

import com.extremecraft.future.registry.TechMenuTypes;
import net.minecraft.world.entity.player.Inventory;

public class DualWieldMenu extends PlayerTabMenu {
    public DualWieldMenu(int containerId, Inventory inventory) {
        super(TechMenuTypes.DUAL_WIELD.get(), containerId, inventory);
    }
}
