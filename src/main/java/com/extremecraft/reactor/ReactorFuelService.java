package com.extremecraft.reactor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

public final class ReactorFuelService {
    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> NUCLEAR_MATERIALS = ItemTags.create(new ResourceLocation("extremecraft", "nuclear_materials"));

    private ReactorFuelService() {
    }

    public static boolean refuel(com.extremecraft.machine.core.TechMachineBlockEntity machine, ReactorControlService.ReactorState state) {
        if (machine == null || state == null || state.fuelTicksRemaining() > 0 || state.meltedDown()) {
            return false;
        }

        ItemStack stack = machine.getItemHandler().getStackInSlot(com.extremecraft.machine.core.TechMachineBlockEntity.FUEL_SLOT);
        FuelProfile profile = profileFor(stack);
        if (profile.burnTicks() <= 0) {
            return false;
        }

        stack.shrink(1);
        state.setFuelTicksRemaining(profile.burnTicks());
        state.setReactivity(profile.reactivity());
        state.setRadiation(Math.max(state.radiation(), profile.radiation()));
        return true;
    }

    public static boolean tickBurn(ReactorControlService.ReactorState state) {
        if (state == null || state.fuelTicksRemaining() <= 0) {
            state.setFuelTicksRemaining(0);
            state.setReactivity(0.0D);
            return false;
        }

        state.setFuelTicksRemaining(state.fuelTicksRemaining() - 1);
        return true;
    }

    private static FuelProfile profileFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FuelProfile.NONE;
        }

        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if (stack.is(NUCLEAR_MATERIALS) || itemId.contains("uranium")) {
            return new FuelProfile(240, 16.0D, 4.0D);
        }
        if (itemId.contains("thorium")) {
            return new FuelProfile(320, 10.0D, 2.6D);
        }
        if (itemId.contains("reactor_core")) {
            return new FuelProfile(480, 26.0D, 8.0D);
        }
        return FuelProfile.NONE;
    }

    private record FuelProfile(int burnTicks, double reactivity, double radiation) {
        private static final FuelProfile NONE = new FuelProfile(0, 0.0D, 0.0D);
    }
}
