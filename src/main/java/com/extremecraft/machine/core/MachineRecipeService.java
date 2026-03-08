package com.extremecraft.machine.core;

import com.extremecraft.config.Config;
import com.extremecraft.machine.recipe.MachineProcessingRecipe;
import com.extremecraft.machine.recipe.ModTechRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class MachineRecipeService {
    private MachineRecipeService() {
    }

    public static Optional<MachineProcessingRecipe> getCurrentRecipe(Level level, TechMachineBlockEntity machine, String machineId) {
        if (level == null || machine == null) {
            return Optional.empty();
        }

        var input = machine.getItemHandler().getStackInSlot(TechMachineBlockEntity.INPUT_SLOT);
        if (input.isEmpty()) {
            machine.setCachedRecipeId("");
            machine.setNextRecipeLookupTick(0L);
            return Optional.empty();
        }

        SimpleContainer inventory = new SimpleContainer(1);
        inventory.setItem(0, input);

        Optional<MachineProcessingRecipe> cached = lookupCachedRecipe(level, machine, machineId, inventory);
        if (cached.isPresent()) {
            return cached;
        }

        long now = level.getGameTime();
        if (now < machine.nextRecipeLookupTick()) {
            return Optional.empty();
        }

        machine.setNextRecipeLookupTick(now + Config.recipeLookupIntervalTicks());
        Optional<MachineProcessingRecipe> found = level.getRecipeManager()
                .getRecipesFor(ModTechRecipeTypes.MACHINE_PROCESSING, inventory, level).stream()
                .filter(recipe -> recipe.machineId().equals(machineId))
                .findFirst();

        machine.setCachedRecipeId(found.map(recipe -> recipe.getId().toString()).orElse(""));
        return found;
    }

    private static Optional<MachineProcessingRecipe> lookupCachedRecipe(Level level, TechMachineBlockEntity machine, String machineId, SimpleContainer inventory) {
        if (machine.cachedRecipeId().isBlank()) {
            return Optional.empty();
        }

        ResourceLocation id = ResourceLocation.tryParse(machine.cachedRecipeId());
        if (id == null) {
            machine.setCachedRecipeId("");
            return Optional.empty();
        }

        return level.getRecipeManager().byKey(id)
                .filter(raw -> raw instanceof MachineProcessingRecipe)
                .map(raw -> (MachineProcessingRecipe) raw)
                .filter(recipe -> recipe.machineId().equals(machineId) && recipe.matches(inventory, level));
    }
}
