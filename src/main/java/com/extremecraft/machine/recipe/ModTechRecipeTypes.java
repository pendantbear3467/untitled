package com.extremecraft.machine.recipe;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModTechRecipeTypes {
    public static final RecipeType<MachineProcessingRecipe> MACHINE_PROCESSING = new RecipeType<>() {
        @Override
        public String toString() {
            return new ResourceLocation(ECConstants.MODID, "machine_processing").toString();
        }
    };

    private ModTechRecipeTypes() {
    }
}
