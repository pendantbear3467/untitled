package com.extremecraft.machines.pulverizer;

import com.extremecraft.config.Config;
import com.extremecraft.machine.sync.MachineStateSyncProvider;
import com.extremecraft.machines.base.AbstractMachineBlockEntity;
import com.extremecraft.machines.recipe.ModRecipeTypes;
import com.extremecraft.machines.recipe.PulverizerRecipe;
import com.extremecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class PulverizerBlockEntity extends AbstractMachineBlockEntity implements MenuProvider, MachineStateSyncProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int DATA_VERSION = 1;

    private int progress = 0;
    private int maxProgress = 120;
    private String cachedRecipeId = "";
    private long nextRecipeLookupTick;

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
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public PulverizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PULVERIZER_BE.get(), pos, state, 2, 100_000, 500, 500);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PulverizerBlockEntity be) {
        if (level.isClientSide || !Config.isMachineEnabled("pulverizer")) {
            return;
        }

        int tickInterval = Config.machineTickInterval();
        if (tickInterval > 1 && ((level.getGameTime() + pos.asLong()) % tickInterval) != 0L) {
            return;
        }

        boolean changed = false;
        Optional<PulverizerRecipe> recipeOpt = be.getCurrentRecipe();
        if (recipeOpt.isEmpty()) {
            if (be.progress != 0) {
                be.progress = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }

        PulverizerRecipe recipe = recipeOpt.get();
        int nextMaxProgress = Math.max(1, recipe.getProcessTime());
        if (be.maxProgress != nextMaxProgress) {
            be.maxProgress = nextMaxProgress;
            changed = true;
        }

        int energyPerTick = Math.max(0, recipe.getEnergyPerTick());
        if (be.energyStorage.getEnergyStored() < energyPerTick) {
            if (changed) {
                be.setChanged();
            }
            return;
        }

        if (!be.canOutput(recipe.getOutput())) {
            if (be.progress != 0) {
                be.progress = 0;
                changed = true;
            }
            if (changed) {
                be.setChanged();
            }
            return;
        }

        be.energyStorage.extractEnergy(energyPerTick, false);
        be.progress++;
        changed = true;

        if (be.progress >= be.maxProgress) {
            be.progress = 0;
            be.craft(recipe);
        }

        if (changed) {
            be.setChanged();
        }
    }

    private Optional<PulverizerRecipe> getCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            cachedRecipeId = "";
            nextRecipeLookupTick = 0L;
            return Optional.empty();
        }

        SimpleContainer inv = new SimpleContainer(1);
        inv.setItem(0, input);

        Optional<PulverizerRecipe> cached = lookupCachedRecipe(inv);
        if (cached.isPresent()) {
            return cached;
        }

        long now = level.getGameTime();
        if (now < nextRecipeLookupTick) {
            return Optional.empty();
        }

        int lookupCooldown = Config.recipeLookupIntervalTicks();
        nextRecipeLookupTick = now + lookupCooldown;

        Optional<PulverizerRecipe> found = level.getRecipeManager().getRecipeFor(ModRecipeTypes.PULVERIZING, inv, level);
        cachedRecipeId = found.map(recipe -> recipe.getId().toString()).orElse("");
        return found;
    }

    private Optional<PulverizerRecipe> lookupCachedRecipe(SimpleContainer inv) {
        if (level == null || cachedRecipeId.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(cachedRecipeId);
        if (id == null) {
            cachedRecipeId = "";
            return Optional.empty();
        }

        return level.getRecipeManager().byKey(id)
                .filter(raw -> raw instanceof PulverizerRecipe)
                .map(raw -> (PulverizerRecipe) raw)
                .filter(recipe -> recipe.matches(inv, level));
    }

    private boolean canOutput(ItemStack output) {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(out, output)) {
            return false;
        }
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
    public CompoundTag machineSyncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("data_version", DATA_VERSION);
        tag.putString("machine", "pulverizer");
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("energy", energyStorage.getEnergyStored());
        return tag;
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
        tag.putInt("data_version", DATA_VERSION);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int dataVersion = tag.getInt("data_version");
        progress = Math.max(0, tag.getInt("progress"));
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        cachedRecipeId = "";
        nextRecipeLookupTick = 0L;

        if (dataVersion <= 0) {
            progress = Math.max(0, progress);
            maxProgress = Math.max(1, maxProgress);
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot != OUTPUT_SLOT;
    }
}
