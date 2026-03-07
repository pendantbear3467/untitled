package com.extremecraft.machine;

import com.extremecraft.energy.EnergyStorageExt;
import com.extremecraft.machine.core.ECEStorageAdapter;
import com.extremecraft.machine.core.IECEStorage;
import com.extremecraft.machine.sync.MachineStateSyncProvider;
import com.extremecraft.machine.sync.MachineSyncIndex;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import javax.annotation.Nullable;

public class MachineBlockEntity extends BlockEntity implements MachineStateSyncProvider {
    private static final int DATA_VERSION = 1;

    private final String definitionId;
    private final ItemStackHandler inputInventory;
    private final ItemStackHandler outputInventory;
    private final CombinedInvWrapper combinedInventory;
    private final EnergyStorageExt energyStorage;
    private final IECEStorage ecStorage;

    private LazyOptional<IItemHandler> itemCap = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    private int processingTicks = 0;
    private String activeRecipeId = "";

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String machineId) {
        super(type, pos, state);
        this.definitionId = machineId == null ? "" : machineId.trim().toLowerCase();

        MachineDefinition definition = MachineRegistry.getMachine(this.definitionId);
        int inputSlots = definition == null ? 1 : definition.inputSlots();
        int outputSlots = definition == null ? 1 : definition.outputSlots();

        this.inputInventory = new ItemStackHandler(inputSlots);
        this.outputInventory = new ItemStackHandler(outputSlots);
        this.combinedInventory = new CombinedInvWrapper(inputInventory, outputInventory);
        this.energyStorage = new EnergyStorageExt(200_000, 2_000, 2_000);
        this.ecStorage = new ECEStorageAdapter(energyStorage);
    }

    public String definitionId() {
        return definitionId;
    }

    public ItemStackHandler inputInventory() {
        return inputInventory;
    }

    public ItemStackHandler outputInventory() {
        return outputInventory;
    }

    public EnergyStorageExt energyStorage() {
        return energyStorage;
    }

    public IECEStorage ecStorage() {
        return ecStorage;
    }

    public int processingTicks() {
        return processingTicks;
    }

    public void setProcessingTicks(int processingTicks) {
        this.processingTicks = Math.max(0, processingTicks);
    }

    public String activeRecipeId() {
        return activeRecipeId;
    }

    public void setActiveRecipeId(String activeRecipeId) {
        this.activeRecipeId = activeRecipeId == null ? "" : activeRecipeId;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (level == null || level.isClientSide) {
            return;
        }

        MachineProcessingLogic.tick(machine);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        itemCap = LazyOptional.of(() -> combinedInventory);
        energyCap = LazyOptional.of(() -> energyStorage);
        MachineSyncIndex.register(level, worldPosition);
    }

    @Override
    public void invalidateCaps() {
        MachineSyncIndex.unregister(level, worldPosition);
        super.invalidateCaps();
        itemCap.invalidate();
        energyCap.invalidate();
    }

    @Override
    public void setRemoved() {
        MachineSyncIndex.unregister(level, worldPosition);
        super.setRemoved();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCap.cast();
        }

        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }

        return super.getCapability(cap, side);
    }

    public CompoundTag syncTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("data_version", DATA_VERSION);
        tag.putString("machine", definitionId);
        tag.putInt("processing_ticks", processingTicks);
        tag.putString("active_recipe", activeRecipeId);
        tag.putInt("energy", energyStorage.getEnergyStored());
        return tag;
    }

    @Override
    public CompoundTag machineSyncTag() {
        return syncTag();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("data_version", DATA_VERSION);
        tag.putString("machine", definitionId);
        tag.put("input_inventory", inputInventory.serializeNBT());
        tag.put("output_inventory", outputInventory.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        tag.putInt("processing_ticks", processingTicks);
        tag.putString("active_recipe", activeRecipeId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        int dataVersion = tag.getInt("data_version");

        inputInventory.deserializeNBT(tag.getCompound("input_inventory"));
        outputInventory.deserializeNBT(tag.getCompound("output_inventory"));

        int storedEnergy = Math.max(0, tag.getInt("energy"));
        energyStorage.setStored(Math.min(energyStorage.getMaxEnergyStored(), storedEnergy));

        processingTicks = Math.max(0, tag.getInt("processing_ticks"));
        activeRecipeId = tag.getString("active_recipe");

        // Legacy saves before versioning are treated as version 0 and loaded defensively above.
        if (dataVersion <= 0 && activeRecipeId == null) {
            activeRecipeId = "";
        }
    }
}
