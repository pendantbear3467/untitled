package com.extremecraft.machine;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stateless machine tick processor.
 *
 * <p>This class owns the server-side machine execution loop: it resolves a valid recipe,
 * checks energy channels, advances progress, and commits inventory mutations atomically at
 * recipe completion. Keeping this logic centralized prevents drift between different machine
 * block entity implementations.</p>
 */
public final class MachineProcessingLogic {
    private MachineProcessingLogic() {
    }

    public static void tick(MachineBlockEntity machine) {
        MachineDefinition definition = MachineRegistry.getMachine(machine.definitionId());
        if (definition == null) {
            return;
        }

        MachineRecipe recipe = resolveRecipe(machine, definition);
        if (recipe == null) {
            machine.setProcessingTicks(0);
            machine.setActiveRecipeId("");
            return;
        }

        int recipeEnergyPerTick = Math.max(definition.energyPerTick(), Math.max(1, recipe.energyCost() / Math.max(1, recipe.processTicks())));
        boolean hasEnergy = EnergySystem.consume(machine.energyStorage(), machine.ecStorage(), recipeEnergyPerTick,
                definition.supportsEcEnergy() ? EnergySystem.Kind.EC : EnergySystem.Kind.FE);

        if (!hasEnergy) {
            return;
        }

        machine.setProcessingTicks(machine.processingTicks() + 1);
        machine.setChanged();

        if (machine.processingTicks() < recipe.processTicks()) {
            return;
        }

        if (!canAcceptOutputs(machine, recipe.output())) {
            machine.setProcessingTicks(recipe.processTicks() - 1);
            return;
        }

        consumeInputs(machine, recipe.input());
        produceOutputs(machine, recipe.output());

        machine.setProcessingTicks(0);
        machine.setActiveRecipeId(recipe.id());
        machine.setChanged();
    }

    private static MachineRecipe resolveRecipe(MachineBlockEntity machine, MachineDefinition definition) {
        MachineRecipe active = MachineRegistry.recipe(machine.activeRecipeId());
        if (active != null && Objects.equals(active.machineId(), definition.id()) && hasInputs(machine, active.input())) {
            return active;
        }

        List<MachineRecipe> candidates = MachineRegistry.recipesForMachine(definition.id());
        for (MachineRecipe candidate : candidates) {
            if (hasInputs(machine, candidate.input())) {
                machine.setActiveRecipeId(candidate.id());
                return candidate;
            }
        }

        return null;
    }

    private static boolean hasInputs(MachineBlockEntity machine, Map<String, Integer> requirements) {
        if (requirements.isEmpty()) {
            return true;
        }

        Map<String, Integer> available = new LinkedHashMap<>();
        for (int slot = 0; slot < machine.inputInventory().getSlots(); slot++) {
            ItemStack stack = machine.inputInventory().getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
            available.merge(id, stack.getCount(), Integer::sum);
        }

        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
            if (available.getOrDefault(requirement.getKey(), 0) < requirement.getValue()) {
                return false;
            }
        }

        return true;
    }

    private static void consumeInputs(MachineBlockEntity machine, Map<String, Integer> requirements) {
        for (Map.Entry<String, Integer> requirement : requirements.entrySet()) {
            int remaining = requirement.getValue();

            for (int slot = 0; slot < machine.inputInventory().getSlots() && remaining > 0; slot++) {
                ItemStack stack = machine.inputInventory().getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }

                String id = String.valueOf(BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (!id.equals(requirement.getKey())) {
                    continue;
                }

                int consumed = Math.min(remaining, stack.getCount());
                stack.shrink(consumed);
                remaining -= consumed;
            }
        }
    }

    private static boolean canAcceptOutputs(MachineBlockEntity machine, Map<String, Integer> outputs) {
        for (Map.Entry<String, Integer> output : outputs.entrySet()) {
            Item item = resolveItem(output.getKey());
            if (item == null) {
                return false;
            }

            ItemStack toInsert = new ItemStack(item, output.getValue());
            ItemStack simulated = ItemHandlerHelper.insertItemStacked(machine.outputInventory(), toInsert, true);
            if (!simulated.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void produceOutputs(MachineBlockEntity machine, Map<String, Integer> outputs) {
        for (Map.Entry<String, Integer> output : outputs.entrySet()) {
            Item item = resolveItem(output.getKey());
            if (item == null) {
                continue;
            }

            ItemStack produced = new ItemStack(item, output.getValue());
            ItemHandlerHelper.insertItemStacked(machine.outputInventory(), produced, false);
        }
    }

    private static Item resolveItem(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) {
            return null;
        }

        Item item = BuiltInRegistries.ITEM.get(key);
        if (item == null || item == Items.AIR) {
            return null;
        }

        return item;
    }
}

