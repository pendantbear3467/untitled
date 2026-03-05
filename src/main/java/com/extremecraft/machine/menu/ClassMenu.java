package com.extremecraft.machine.menu;

import com.extremecraft.future.registry.TechMenuTypes;
import net.minecraft.world.entity.player.Inventory;

public class ClassMenu extends PlayerTabMenu {
    public ClassMenu(int containerId, Inventory inventory) {
        super(TechMenuTypes.CLASS.get(), containerId, inventory);
    }
}
