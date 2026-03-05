package com.extremecraft.machine.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public abstract class PlayerTabMenu extends AbstractContainerMenu {
    protected final Inventory inventory;

    protected PlayerTabMenu(MenuType<?> menuType, int containerId, Inventory inventory) {
        super(menuType, containerId);
        this.inventory = inventory;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player;
    }

    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int index) {
        return net.minecraft.world.item.ItemStack.EMPTY;
    }
}
