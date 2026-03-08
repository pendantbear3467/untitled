package com.extremecraft.machine.core;

import com.extremecraft.reactor.ReactorControlService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeHooks;

public final class GeneratorLogic {
    private GeneratorLogic() {
    }

    public static boolean tickGenerator(Level level, BlockPos pos, TechMachineBlockEntity machine, MachineDefinition definition) {
        if (ReactorControlService.isReactorController(definition.id())) {
            return ReactorControlService.tickController(level, pos, machine);
        }

        int beforeEnergy = machine.getEnergyStorageExt().getEnergyStored();
        int beforeFuelBurnTime = machine.fuelBurnTime();
        int beforeFuelBurnTimeTotal = machine.fuelBurnTimeTotal();
        int generation = Math.max(0, definition.generationPerTick());

        if ("coal_generator".equals(definition.id()) || "industrial_generator".equals(definition.id()) || "steam_generator".equals(definition.id())) {
            if (machine.fuelBurnTime() > 0) {
                machine.setFuelBurnTime(machine.fuelBurnTime() - 1);
                machine.getEnergyStorageExt().receiveEnergy(generation, false);
                return stateChanged(machine, beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
            }

            ItemStack fuel = machine.getItemHandler().getStackInSlot(TechMachineBlockEntity.FUEL_SLOT);
            if (!fuel.isEmpty() && (fuel.is(Items.COAL) || fuel.is(Items.CHARCOAL))) {
                int burn = ForgeHooks.getBurnTime(fuel, null);
                if (burn > 0) {
                    fuel.shrink(1);
                    machine.setFuelBurnTime(burn);
                    machine.setFuelBurnTimeTotal(burn);
                }
            }

            return stateChanged(machine, beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
        }

        if ("solar_generator".equals(definition.id())) {
            if (level != null && level.isDay() && level.canSeeSky(pos.above())) {
                machine.getEnergyStorageExt().receiveEnergy(generation, false);
            }
            return stateChanged(machine, beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
        }

        machine.getEnergyStorageExt().receiveEnergy(generation, false);
        return stateChanged(machine, beforeEnergy, beforeFuelBurnTime, beforeFuelBurnTimeTotal);
    }

    private static boolean stateChanged(TechMachineBlockEntity machine, int beforeEnergy, int beforeFuelBurnTime, int beforeFuelBurnTimeTotal) {
        return beforeEnergy != machine.getEnergyStorageExt().getEnergyStored()
                || beforeFuelBurnTime != machine.fuelBurnTime()
                || beforeFuelBurnTimeTotal != machine.fuelBurnTimeTotal();
    }
}
