package com.extremecraft.machine.core;

import com.extremecraft.energy.EnergyStorageExt;
import com.extremecraft.future.registry.TechBlockEntities;
import com.extremecraft.machine.menu.TechMachineMenu;
import com.extremecraft.machine.recipe.MachineProcessingRecipe;
import com.extremecraft.machine.recipe.ModTechRecipeTypes;
import com.extremecraft.machines.base.AbstractMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Optional;

public class TechMachineBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int FUEL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;

    private int progress;
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
        if (level.isClientSide) {
            return;
        }

        MachineDefinition definition = be.getMachineDefinition();
        if (definition.category() == MachineCategory.GENERATOR) {
            be.tickGenerator(definition);
        } else {
            be.tickProcessor(definition);
        }

        be.pushEnergyToNeighbors();
        be.setChanged();
    }

    private void tickGenerator(MachineDefinition definition) {
        int generation = definition.generationPerTick();

        if ("coal_generator".equals(definition.id()) || "industrial_generator".equals(definition.id())) {
            ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
            if (!fuel.isEmpty() && (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL))) {
                fuel.shrink(1);
                energyStorage.receiveEnergy(generation * 20, false);
            }
            return;
        }

        if ("steam_generator".equals(definition.id())) {
            ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
            if (!fuel.isEmpty() && fuel.is(Items.COAL)) {
                fuel.shrink(1);
                energyStorage.receiveEnergy(generation * 30, false);
            }
            return;
        }

        if ("solar_generator".equals(definition.id())) {
            if (level != null && level.isDay() && level.canSeeSky(worldPosition.above())) {
                energyStorage.receiveEnergy(generation, false);
            }
            return;
        }

        energyStorage.receiveEnergy(generation, false);
    }

    private void tickProcessor(MachineDefinition definition) {
        Optional<MachineProcessingRecipe> recipe = getCurrentRecipe(definition.id());
        ItemStack produced;
        int ept;

        if (recipe.isPresent()) {
            MachineProcessingRecipe current = recipe.get();
            maxProgress = Math.max(1, current.processTime());
            ept = Math.max(definition.energyPerTick(), current.energyPerTick());
            produced = current.output();
        } else {
            Optional<ItemStack> fallback = fallbackOutput(definition);
            if (fallback.isEmpty()) {
                progress = 0;
                return;
            }
            maxProgress = Math.max(1, definition.processTime());
            ept = Math.max(20, definition.energyPerTick());
            produced = fallback.get();
        }

        produced.setCount(produced.getCount() * Math.max(1, definition.outputMultiplier()));

        if (energyStorage.getEnergyStored() < ept || !canOutput(produced)) {
            if (!canOutput(produced)) {
                progress = 0;
            }
            return;
        }

        energyStorage.extractEnergy(ept, false);
        progress++;

        if (progress >= maxProgress) {
            progress = 0;
            craft(produced);
        }
    }

    private Optional<ItemStack> fallbackOutput(MachineDefinition definition) {
        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return Optional.empty();
        }

        ResourceLocation inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        if (inputId == null) {
            return Optional.empty();
        }

        String path = inputId.getPath();

        if ((definition.id().contains("pulverizer") || definition.id().contains("crusher")) && path.endsWith("_ore")) {
            String dustPath = path.substring(0, path.length() - 4) + "_dust";
            Item dust = BuiltInRegistries.ITEM.get(new ResourceLocation(inputId.getNamespace(), dustPath));
            if (dust != Items.AIR) {
                return Optional.of(new ItemStack(dust, definition.outputMultiplier()));
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

    private Optional<MachineProcessingRecipe> getCurrentRecipe(String machineId) {
        if (level == null) {
            return Optional.empty();
        }

        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, itemHandler.getStackInSlot(INPUT_SLOT));

        return level.getRecipeManager().getAllRecipesFor(ModTechRecipeTypes.MACHINE_PROCESSING).stream()
                .filter(recipe -> recipe.machineId().equals(machineId) && recipe.matches(inventory, level))
                .findFirst();
    }

    private void craft(ItemStack output) {
        itemHandler.extractItem(INPUT_SLOT, 1, false);
        ItemStack currentOutput = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (currentOutput.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, output.copy());
            return;
        }

        currentOutput.grow(output.getCount());
        itemHandler.setStackInSlot(OUTPUT_SLOT, currentOutput);
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

    private void pushEnergyToNeighbors() {
        if (level == null || energyStorage.getEnergyStored() <= 0) {
            return;
        }

        for (Direction direction : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(direction);
            if (level.getBlockEntity(targetPos) == null) {
                continue;
            }

            level.getBlockEntity(targetPos)
                    .getCapability(ForgeCapabilities.ENERGY, direction.getOpposite())
                    .ifPresent(storage -> transferTo(storage, 1200));
        }
    }

    private void transferTo(IEnergyStorage target, int maxTransfer) {
        if (!target.canReceive() || energyStorage.getEnergyStored() <= 0) {
            return;
        }

        int extracted = energyStorage.extractEnergy(maxTransfer, true);
        if (extracted <= 0) {
            return;
        }

        int accepted = target.receiveEnergy(extracted, false);
        if (accepted > 0) {
            energyStorage.extractEnergy(accepted, false);
        }
    }

    public String getMachineId() {
        return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath();
    }

    private MachineDefinition getMachineDefinition() {
        return MachineCatalog.byId(getMachineId()).orElse(new MachineDefinition(getMachineId(), MachineCategory.PROCESSOR,
                com.extremecraft.progression.stage.ProgressionStage.INDUSTRIAL, 120, 20, 1, 0));
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
            String machineId = getMachineId();
            if (!(machineId.endsWith("generator") || machineId.contains("reactor"))) {
                return false;
            }
            return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
        }

        return true;
    }
}
