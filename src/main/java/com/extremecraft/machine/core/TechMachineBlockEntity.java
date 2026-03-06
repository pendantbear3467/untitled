package com.extremecraft.machine.core;

import com.extremecraft.config.Config;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
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
        if (level.isClientSide) {
            return;
        }

        if (!Config.isMachineEnabled(be.getMachineId())) {
            return;
        }

        int tickInterval = Math.max(1, Config.COMMON.machines.machineTickInterval.get());
        if (tickInterval > 1 && ((level.getGameTime() + pos.asLong()) % tickInterval) != 0L) {
            return;
        }

        MachineDefinition definition = be.getMachineDefinition();
        boolean changed = definition.category() == MachineCategory.GENERATOR
                ? be.tickGenerator(definition)
                : be.tickProcessor(definition);

        int transferPerSide = Math.max(0, Config.COMMON.machines.neighborEnergyPushPerSide.get());
        changed |= be.pushEnergyToNeighbors(transferPerSide);

        if (changed) {
            be.setChanged();
        }
    }

    private boolean tickGenerator(MachineDefinition definition) {
        int beforeEnergy = energyStorage.getEnergyStored();
        int beforeFuelBurnTime = fuelBurnTime;
        int beforeFuelBurnTimeTotal = fuelBurnTimeTotal;
        int generation = Math.max(0, definition.generationPerTick());

        if ("coal_generator".equals(definition.id()) || "industrial_generator".equals(definition.id()) || "steam_generator".equals(definition.id())) {
            if (fuelBurnTime > 0) {
                fuelBurnTime--;
                energyStorage.receiveEnergy(generation, false);
                return hasGeneratorStateChanged(beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
            }

            ItemStack fuel = itemHandler.getStackInSlot(FUEL_SLOT);
            if (!fuel.isEmpty() && (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL))) {
                int burn = ForgeHooks.getBurnTime(fuel, null);
                if (burn > 0) {
                    fuel.shrink(1);
                    fuelBurnTime = burn;
                    fuelBurnTimeTotal = burn;
                }
            }

            return hasGeneratorStateChanged(beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
        }

        if ("solar_generator".equals(definition.id())) {
            if (level != null && level.isDay() && level.canSeeSky(worldPosition.above())) {
                energyStorage.receiveEnergy(generation, false);
            }
            return hasGeneratorStateChanged(beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
        }

        energyStorage.receiveEnergy(generation, false);
        return hasGeneratorStateChanged(beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
    }

    private boolean hasGeneratorStateChanged(int beforeEnergy, int beforeFuelBurnTime, int beforeFuelBurnTimeTotal) {
        return beforeEnergy != energyStorage.getEnergyStored()
                || beforeFuelBurnTime != fuelBurnTime
                || beforeFuelBurnTimeTotal != fuelBurnTimeTotal;
    }

    private boolean tickProcessor(MachineDefinition definition) {
        boolean changed = false;

        Optional<MachineProcessingRecipe> recipe = getCurrentRecipe(definition.id());
        ItemStack produced;
        int energyPerTick;

        if (recipe.isPresent()) {
            MachineProcessingRecipe current = recipe.get();
            maxProgress = Math.max(1, current.processTime());
            energyPerTick = Math.max(definition.energyPerTick(), current.energyPerTick());
            produced = current.output();
        } else {
            if (!Config.COMMON.machines.enableFallbackRecipes.get()) {
                if (progress != 0) {
                    progress = 0;
                    changed = true;
                }
                return changed;
            }

            Optional<ItemStack> fallback = fallbackOutput(definition);
            if (fallback.isEmpty()) {
                if (progress != 0) {
                    progress = 0;
                    changed = true;
                }
                return changed;
            }

            maxProgress = Math.max(1, definition.processTime());
            energyPerTick = Math.max(20, definition.energyPerTick());
            produced = fallback.get();
        }

        produced.setCount(produced.getCount() * Math.max(1, definition.outputMultiplier()));

        if (energyStorage.getEnergyStored() < energyPerTick || !canOutput(produced)) {
            if (!canOutput(produced) && progress != 0) {
                progress = 0;
                changed = true;
            }
            return changed;
        }

        int extracted = energyStorage.extractEnergy(energyPerTick, false);
        if (extracted <= 0) {
            return changed;
        }

        progress++;
        changed = true;

        if (progress >= maxProgress) {
            progress = 0;
            craft(produced);
        }

        return changed;
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

    private Optional<MachineProcessingRecipe> getCurrentRecipe(String machineId) {
        if (level == null) {
            return Optional.empty();
        }

        ItemStack input = itemHandler.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            cachedRecipeId = "";
            nextRecipeLookupTick = 0L;
            return Optional.empty();
        }

        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, input);

        Optional<MachineProcessingRecipe> cached = lookupCachedRecipe(machineId, inventory);
        if (cached.isPresent()) {
            return cached;
        }

        long now = level.getGameTime();
        if (now < nextRecipeLookupTick) {
            return Optional.empty();
        }

        int lookupCooldown = Math.max(1, Config.COMMON.machines.recipeLookupIntervalTicks.get());
        nextRecipeLookupTick = now + lookupCooldown;

        Optional<MachineProcessingRecipe> found = level.getRecipeManager().getAllRecipesFor(ModTechRecipeTypes.MACHINE_PROCESSING).stream()
                .filter(recipe -> recipe.machineId().equals(machineId) && recipe.matches(inventory, level))
                .findFirst();

        cachedRecipeId = found.map(recipe -> recipe.getId().toString()).orElse("");
        return found;
    }

    private Optional<MachineProcessingRecipe> lookupCachedRecipe(String machineId, SimpleContainer inventory) {
        if (level == null || cachedRecipeId.isBlank()) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(cachedRecipeId);
        if (id == null) {
            cachedRecipeId = "";
            return Optional.empty();
        }

        return level.getRecipeManager().byKey(id)
                .filter(raw -> raw instanceof MachineProcessingRecipe)
                .map(raw -> (MachineProcessingRecipe) raw)
                .filter(recipe -> recipe.machineId().equals(machineId) && recipe.matches(inventory, level));
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

    private boolean pushEnergyToNeighbors(int maxTransferPerSide) {
        if (level == null || maxTransferPerSide <= 0 || energyStorage.getEnergyStored() <= 0) {
            return false;
        }

        boolean changed = false;
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = worldPosition.relative(direction);
            BlockEntity targetBe = level.getBlockEntity(targetPos);
            if (targetBe == null) {
                continue;
            }

            IEnergyStorage target = targetBe.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null) {
                continue;
            }

            changed |= transferTo(target, maxTransferPerSide);
            if (energyStorage.getEnergyStored() <= 0) {
                break;
            }
        }

        return changed;
    }

    private boolean transferTo(IEnergyStorage target, int maxTransfer) {
        if (!target.canReceive() || energyStorage.getEnergyStored() <= 0) {
            return false;
        }

        int extracted = energyStorage.extractEnergy(maxTransfer, true);
        if (extracted <= 0) {
            return false;
        }

        int accepted = target.receiveEnergy(extracted, false);
        if (accepted > 0) {
            energyStorage.extractEnergy(accepted, false);
            return true;
        }

        return false;
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
        tag.putInt("fuel_burn_time", fuelBurnTime);
        tag.putInt("fuel_burn_time_total", fuelBurnTimeTotal);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        maxProgress = Math.max(1, tag.getInt("max_progress"));
        fuelBurnTime = Math.max(0, tag.getInt("fuel_burn_time"));
        fuelBurnTimeTotal = Math.max(0, tag.getInt("fuel_burn_time_total"));
        cachedRecipeId = "";
        nextRecipeLookupTick = 0L;
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
