package com.extremecraft.machines.pulverizer;

import com.extremecraft.machines.base.AbstractMachineBlockEntity;
import com.extremecraft.machines.recipe.ModRecipeTypes;
import com.extremecraft.machines.recipe.PulverizerRecipe;
import com.extremecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public class PulverizerBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private static final Map<Item, Integer> FUEL_FE = Map.of(
            Items.COAL, 1600,
            Items.CHARCOAL, 1600,
            Items.BLAZE_ROD, 2400,
            Items.REDSTONE, 400
    );

    private int progress = 0;
    private int maxProgress = 120;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energyStorage.getEnergyStored();
                case 3 -> energyStorage.getMaxEnergyStored();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULVERIZER_BE.get(), pos, state, 3, 100_000, 500, 500);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity be) {
        if (level.isClientSide) return;

        be.burnFuelIfNeeded();

        Optional<PulverizerRecipe> recipeOpt = be.getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            be.progress = 0;
            be.setChanged();
            return;
        }

        PulverizerRecipe recipe = recipeOpt.get();
        be.maxProgress = recipe.getProcessTime();

        if (be.energyStorage.getEnergyStored() < recipe.getEnergyPerTick()) {
            be.setChanged();
            return;
        }

        if (!be.canOutput(recipe.getOutput())) {
            be.progress = 0;
            be.setChanged();
            return;
        }

        be.energyStorage.extractEnergy(recipe.getEnergyPerTick(), false);
        be.progress++;

        if (be.progress >= be.maxProgress) {
            be.progress = 0;
            be.craft(recipe);
        }

        be.setChanged();
    }

    private void burnFuelIfNeeded() {
        if (energyStorage.getEnergyStored() > 90_000) return;

        ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
        if (fuel.isEmpty()) return;

        Integer fe = FUEL_FE.get(fuel.getItem());
        if (fe == null || fe <= 0) return;

        fuel.shrink(1);
        energyStorage.receiveEnergy(fe, false);
    }

    private Optional<PulverizerRecipe> getCurrentRecipe() {
        if (level == null) return Optional.empty();

        SimpleContainer inv = new SimpleContainer(1);
        inv.setItem(0, itemHandler.getStackInSlot(INPUT_SLOT));
        return level.getRecipeManager().getRecipeFor(ModRecipeTypes.PULVERIZING, inv, level);
    }

    private boolean canOutput(ItemStack output) {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(out, output)) return false;
        return out.getCount() + output.getCount() <= out.getMaxStackSize();
    }

    private void craft(PulverizerRecipe recipe) {
        itemHandler.extractItem(INPUT_SLOT, 1, false);
        ItemStack output = recipe.getOutput();
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (out.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
        } else {
            out.grow(output.getCount());
            itemHandler.setStackInSlot(OUTPUT_SLOT, out);
        }
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public com.extremecraft.energy.EnergyStorageExt getEnergy() {
        return energyStorage;
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.extremecraft.pulverizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PulverizerMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return false;
        }
        if (slot == FUEL_SLOT) {
            return FUEL_FE.containsKey(stack.getItem());
        }
        return true;
    }
}
