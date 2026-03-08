package com.extremecraft.machine.core;

import com.extremecraft.energy.EnergyStorageExt;
import com.extremecraft.future.registry.TechBlockEntities;
import com.extremecraft.machine.menu.TechMachineMenu;
import com.extremecraft.machine.sync.MachineStateSyncProvider;
import com.extremecraft.machines.base.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
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
import java.util.Optional;

public class TechMachineBlockEntity extends AbstractMachineBlockEntity implements MenuProvider, MachineStateSyncProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    private static final int DATA_VERSION = 1;

    private int progress;
    private int maxProgress = 120;
    private int fuelBurnTime;
    private int fuelBurnTimeTotal;
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

    public TechMachineBlockEntity(BlockPos pos, BlockState state) {
        super(TechBlockEntities.TECH_MACHINE.get(), pos, state, 3, 400_000, 3_000, 3_000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TechMachineBlockEntity be) {
        MachineTickScheduler.serverTick(level, pos, be);
    }

    MachineDefinition getMachineDefinition() {
        return MachineCatalog.byId(getMachineId()).orElse(new MachineDefinition(
                getMachineId(),
                MachineCategory.PROCESSOR,
                com.extremecraft.progression.stage.ProgressionStage.INDUSTRIAL,
                120,
                20,
                1,
                0
        ));
    }

    Optional<ItemStack> fallbackOutput(MachineDefinition definition) {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (inputId == null) {
            return Optional.empty();
        }

        String path = inputId.getPath();
        if (definition.id().contains("crusher") && path.endsWith("_ore")) {
            String base = path.substring(0, path.length() - 4);
            if (base.startsWith("deepslate_")) {
                base = base.substring("deepslate_".length());
            }

            String rawPath = "raw_" + base;
            Item raw = BuiltInRegistries.ITEM.get(new ResourceLocation(inputId.getNamespace(), rawPath));
            if (raw != Items.AIR) {
                return Optional.of(new ItemStack(raw, 1));
            }
        }

        if ((definition.id().contains("furnace") || definition.id().contains("smelter") || definition.id().contains("enrichment")) && path.endsWith("_dust")) {
            String ingotPath = path.substring(0, path.length() - 5) + "_ingot";
            Item ingot = BuiltInRegistries.ITEM.get(new ResourceLocation(inputId.getNamespace(), ingotPath));
            if (ingot != Items.AIR) {
                int count = definition.id().contains("enrichment") ? 2 : 1;
                return Optional.of(new ItemStack(ingot, count));
            }
        }

        return Optional.empty();
    }

    void craft(ItemStack output) {
        itemHandler.extractItem(INPUT_SLOT, 1, false);
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
            return;
        }

        currentOutput.grow(output.getCount());
        itemHandler.setStackInSlot(OUTPUT_SLOT, currentOutput);
    }

    boolean canOutput(ItemStack output) {
        ItemStack out = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (out.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameTags(out, output)) {
            return false;
        }
        return out.getCount() + output.getCount() <= out.getMaxStackSize();
    }

    int progress() {
        return progress;
    }

    void setProgressValue(int value) {
        progress = Math.max(0, value);
    }

    int maxProgress() {
        return maxProgress;
    }

    void setMaxProgressValue(int value) {
        maxProgress = Math.max(1, value);
    }

    int fuelBurnTime() {
        return fuelBurnTime;
    }

    void setFuelBurnTime(int value) {
        fuelBurnTime = Math.max(0, value);
    }

    int fuelBurnTimeTotal() {
        return fuelBurnTimeTotal;
    }

    void setFuelBurnTimeTotal(int value) {
        fuelBurnTimeTotal = Math.max(0, value);
    }

    String cachedRecipeId() {
        return cachedRecipeId;
    }

    void setCachedRecipeId(String value) {
        cachedRecipeId = value == null ? "" : value;
    }

    long nextRecipeLookupTick() {
        return nextRecipeLookupTick;
    }

    void setNextRecipeLookupTick(long value) {
        nextRecipeLookupTick = Math.max(0L, value);
    }

    public String getMachineId() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public EnergyStorageExt getEnergyStorageExt() {
        return energyStorage;
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    public CompoundTag machineSyncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("data_version", DATA_VERSION);
        tag.putString("machine", getMachineId());
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("fuel_burn_time", fuelBurnTime);
        tag.putInt("fuel_burn_time_total", fuelBurnTimeTotal);
        tag.putInt("energy", energyStorage.getEnergyStored());
        return tag;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.extremecraft." + getMachineId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TechMachineMenu(containerId, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.putInt("data_version", DATA_VERSION);
        tag.putInt("progress", progress);
        tag.putInt("max_progress", maxProgress);
        tag.putInt("fuel_burn_time", fuelBurnTime);
        tag.putInt("fuel_burn_time_total", fuelBurnTimeTotal);
        tag.putString("cached_recipe_id", cachedRecipeId);
        tag.putLong("next_recipe_lookup_tick", nextRecipeLookupTick);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int dataVersion = tag.getInt("data_version");
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        fuelBurnTime = Math.max(0, tag.getInt("fuel_burn_time"));
        fuelBurnTimeTotal = Math.max(0, tag.getInt("fuel_burn_time_total"));
        cachedRecipeId = tag.getString("cached_recipe_id");
        nextRecipeLookupTick = Math.max(0L, tag.getLong("next_recipe_lookup_tick"));

        if (dataVersion <= 0) {
            progress = Math.max(0, progress);
            maxProgress = Math.max(1, maxProgress);
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == OUTPUT_SLOT) {
            return false;
        }

        if (slot == FUEL_SLOT) {
            String machineId = getMachineId();
            if (!(machineId.endsWith("generator") || machineId.contains("reactor"))) {
                return false;
            }
            return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.getDescriptionId().contains("uranium") || stack.getDescriptionId().contains("thorium");
        }

        return true;
    }
}
