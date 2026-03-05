package com.extremecraft.machine.menu;

import com.extremecraft.future.registry.TechMenuTypes;
import com.extremecraft.machine.core.TechMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class TechMachineMenu extends AbstractContainerMenu {
    private final TechMachineBlockEntity blockEntity;
    private final ContainerData data;

    public TechMachineMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (TechMachineBlockEntity) inventory.player.level().getBlockEntity(pos), new SimpleContainerData(4));
    }

    public TechMachineMenu(int containerId, Inventory inventory, TechMachineBlockEntity blockEntity, ContainerData data) {
        super(TechMenuTypes.TECH_MACHINE.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), TechMachineBlockEntity.INPUT_SLOT, 44, 35));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), TechMachineBlockEntity.FUEL_SLOT, 8, 53));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), TechMachineBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        addDataSlots(data);
    }

    public int progress() {
        int current = data.get(0);
        int max = Math.max(1, data.get(1));
        return current * 24 / max;
    }

    public int energy() {
        int current = data.get(2);
        int max = Math.max(1, data.get(3));
        return current * 52 / max;
    }

    public String machineId() {
        return blockEntity.getMachineId();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !player.isRemoved() && blockEntity != null && player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }
}
