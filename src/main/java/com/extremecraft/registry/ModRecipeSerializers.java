package com.extremecraft.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machines.recipe.PulverizerRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ECConstants.MODID);

    public static final RegistryObject<RecipeSerializer<?>> PULVERIZING = RECIPE_SERIALIZERS.register("pulverizing", PulverizerRecipe.Serializer::new);

    private ModRecipeSerializers() {}
}
