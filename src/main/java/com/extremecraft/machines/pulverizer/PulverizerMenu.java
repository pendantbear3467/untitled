package com.extremecraft.machines.pulverizer;

import com.extremecraft.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class PulverizerMenu extends AbstractContainerMenu {
    private final PulverizerBlockEntity blockEntity;
    private final ContainerData data;

    public PulverizerMenu(int containerId, Inventory inventory, BlockPos pos) {
        this(containerId, inventory, (PulverizerBlockEntity) inventory.player.level().getBlockEntity(pos), new SimpleContainerData(4));
    }

    public PulverizerMenu(int containerId, Inventory inventory, PulverizerBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.PULVERIZER_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), PulverizerBlockEntity.INPUT_SLOT, 56, 35));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), PulverizerBlockEntity.FUEL_SLOT, 8, 53));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), PulverizerBlockEntity.OUTPUT_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getProgressScaled() {
        int progress = data.get(0);
        int max = data.get(1);
        int width = 24;
        return max == 0 ? 0 : progress * width / max;
    }

    public int getEnergyScaled() {
        int energy = data.get(2);
        int max = data.get(3);
        int height = 52;
        return max == 0 ? 0 : energy * height / max;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < 3) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(stack, 0, 2, false)) {
                if (index < 30) {
                    if (!moveItemStackTo(stack, 30, 39, false)) return ItemStack.EMPTY;
                } else if (!moveItemStackTo(stack, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }
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
        return !player.isRemoved() && player.level().getBlockEntity(blockEntity.getBlockPos()) == blockEntity;
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
