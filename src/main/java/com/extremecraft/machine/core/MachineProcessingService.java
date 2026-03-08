package com.extremecraft.machine.core;

import com.extremecraft.config.Config;
import com.extremecraft.magic.AetherNetworkService;
import com.extremecraft.machine.recipe.MachineProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;

public final class MachineProcessingService {
    private MachineProcessingService() {
    }

    public static boolean tickProcessor(Level level, BlockPos pos, TechMachineBlockEntity machine, MachineDefinition definition) {
        boolean changed = false;

        Optional<MachineProcessingRecipe> recipe = MachineRecipeService.getCurrentRecipe(level, machine, definition.id());
        ItemStack produced;
        int energyPerTick;

        if (recipe.isPresent()) {
            MachineProcessingRecipe current = recipe.get();
            machine.setMaxProgressValue(Math.max(1, current.processTime()));
            energyPerTick = Math.max(definition.energyPerTick(), current.energyPerTick());
            produced = current.output();
        } else {
            if (!Config.areFallbackMachineRecipesEnabled()) {
                if (machine.progress() != 0) {
                    machine.setProgressValue(0);
                    changed = true;
                }
                return changed;
            }

            Optional<ItemStack> fallback = machine.fallbackOutput(definition);
            if (fallback.isEmpty()) {
                if (machine.progress() != 0) {
                    machine.setProgressValue(0);
                    changed = true;
                }
                return changed;
            }

            machine.setMaxProgressValue(Math.max(1, definition.processTime()));
            energyPerTick = Math.max(20, definition.energyPerTick());
            produced = fallback.get();
        }

        produced.setCount(produced.getCount() * Math.max(1, definition.outputMultiplier()));
        if (machine.getEnergyStorageExt().getEnergyStored() < energyPerTick || !machine.canOutput(produced)) {
            if (!machine.canOutput(produced) && machine.progress() != 0) {
                machine.setProgressValue(0);
                changed = true;
            }
            return changed;
        }

        int aetherCost = AetherNetworkService.machineAetherCost(definition);
        if (aetherCost > 0 && !AetherNetworkService.tryConsume(level, pos, aetherCost)) {
            if (machine.progress() != 0) {
                machine.setProgressValue(0);
                changed = true;
            }
            return changed;
        }

        int extracted = machine.getEnergyStorageExt().extractEnergy(energyPerTick, false);
        if (extracted <= 0) {
            return changed;
        }

        machine.setProgressValue(machine.progress() + 1);
        changed = true;

        if (machine.progress() >= machine.maxProgress()) {
            machine.setProgressValue(0);
            machine.craft(produced);
        }

        return changed;
    }
}
