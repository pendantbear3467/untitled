package com.extremecraft.client.gui.machine;

import com.extremecraft.machine.menu.TechMachineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReactorMachineScreen extends TechMachineScreen {
    public ReactorMachineScreen(TechMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected MachineFamily machineFamily() {
        return MachineFamily.REACTOR;
    }
}
