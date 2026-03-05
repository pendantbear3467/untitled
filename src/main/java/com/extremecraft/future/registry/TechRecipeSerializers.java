package com.extremecraft.future.registry;

import com.extremecraft.core.ECConstants;
import com.extremecraft.magic.recipe.HybridCraftingRecipe;
import com.extremecraft.machine.recipe.MachineProcessingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class TechRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ECConstants.MODID);

    public static final RegistryObject<RecipeSerializer<?>> MACHINE_PROCESSING = RECIPE_SERIALIZERS.register("machine_processing", MachineProcessingRecipe.Serializer::new);
    public static final RegistryObject<RecipeSerializer<?>> HYBRID_CRAFTING = RECIPE_SERIALIZERS.register("hybrid_crafting", HybridCraftingRecipe.Serializer::new);

    private TechRecipeSerializers() {
    }
}
