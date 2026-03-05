package com.extremecraft.gui;

import com.extremecraft.gui.framework.AnimatedEnergyBar;
import com.extremecraft.gui.framework.AnimatedProgressBar;
import com.extremecraft.gui.framework.BaseMachineScreen;
import com.extremecraft.machines.pulverizer.PulverizerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class PulverizerScreen extends BaseMachineScreen<PulverizerMenu> {
    public PulverizerScreen(PulverizerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);

        this.progressBar = new AnimatedProgressBar(
                79,
                34,
                24,
                16,
                menu::getProgressScaled,
                menu::getProgress,
                menu::getMaxProgress
        );

        this.energyBar = new AnimatedEnergyBar(
                8,
                18,
                6,
                52,
                menu::getEnergyScaled,
                menu::getEnergyStored,
                menu::getMaxEnergy
        );

        addMachineSlotHighlight(56, 35); // input
        addMachineSlotHighlight(8, 53);  // fuel
        addMachineSlotHighlight(116, 35); // output
    }

    @Override
    protected boolean isMachineActive() {
        return menu.isCrafting();
    }
}
