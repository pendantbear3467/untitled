package com.extremecraft.machines.recipe;

import com.extremecraft.core.ECConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipeTypes {
    public static final RecipeType<PulverizerRecipe> PULVERIZING = new RecipeType<>() {
        @Override
        public String toString() {
            return new ResourceLocation(ECConstants.MODID, "pulverizing").toString();
        }
    };

    private ModRecipeTypes() {}
}
