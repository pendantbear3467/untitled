package com.extremecraft.magic.recipe;

import com.extremecraft.future.registry.TechRecipeSerializers;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

public class HybridCraftingRecipe implements Recipe<SimpleContainer> {
    public static final RecipeType<HybridCraftingRecipe> TYPE = new RecipeType<>() {
    };

    private final ResourceLocation id;
    private final Ingredient techInput;
    private final Ingredient magicInput;
    private final ItemStack output;
    private final int manaCost;
    private final int energyCost;

    public HybridCraftingRecipe(ResourceLocation id, Ingredient techInput, Ingredient magicInput, ItemStack output, int manaCost, int energyCost) {
        this.id = id;
        this.techInput = techInput;
        this.magicInput = magicInput;
        this.output = output;
        this.manaCost = manaCost;
        this.energyCost = energyCost;
    }

    public int manaCost() {
        return manaCost;
    }

    public int energyCost() {
        return energyCost;
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return techInput.test(container.getItem(0)) && magicInput.test(container.getItem(1));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, net.minecraft.core.RegistryAccess access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess access) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return TechRecipeSerializers.HYBRID_CRAFTING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(techInput);
        ingredients.add(magicInput);
        return ingredients;
    }

    public static class Serializer implements RecipeSerializer<HybridCraftingRecipe> {
        @Override
        public HybridCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient tech = Ingredient.fromJson(GsonHelper.getNonNull(json, "tech_input"));
            Ingredient magic = Ingredient.fromJson(GsonHelper.getNonNull(json, "magic_input"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
            int mana = GsonHelper.getAsInt(json, "mana_cost", 100);
            int energy = GsonHelper.getAsInt(json, "energy_cost", 1000);
            return new HybridCraftingRecipe(id, tech, magic, output, mana, energy);
        }

        @Override
        public HybridCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient tech = Ingredient.fromNetwork(buf);
            Ingredient magic = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            int mana = buf.readVarInt();
            int energy = buf.readVarInt();
            return new HybridCraftingRecipe(id, tech, magic, output, mana, energy);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, HybridCraftingRecipe recipe) {
            recipe.techInput.toNetwork(buf);
            recipe.magicInput.toNetwork(buf);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.manaCost);
            buf.writeVarInt(recipe.energyCost);
        }
    }
}
