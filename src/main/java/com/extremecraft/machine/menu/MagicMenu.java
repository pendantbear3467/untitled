package com.extremecraft.machine.menu;

import com.extremecraft.future.registry.TechMenuTypes;
import net.minecraft.world.entity.player.Inventory;

public class MagicMenu extends PlayerTabMenu {
    public MagicMenu(int containerId, Inventory inventory) {
        super(TechMenuTypes.MAGIC.get(), containerId, inventory);
    }
}
