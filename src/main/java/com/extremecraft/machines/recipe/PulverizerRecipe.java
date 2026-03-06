package com.extremecraft.machines.recipe;

import com.extremecraft.core.ECConstants;
import com.extremecraft.registry.ModRecipeSerializers;
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

public class PulverizerRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Ingredient input;
    private final ItemStack output;
    private final int processTime;
    private final int energyPerTick;

    public PulverizerRecipe(ResourceLocation id, Ingredient input, ItemStack output, int processTime, int energyPerTick) {
        this.id = id;
        this.input = input;
        this.output = output;
        this.processTime = processTime;
        this.energyPerTick = energyPerTick;
    }

    public Ingredient getInput() { return input; }
    public ItemStack getOutput() { return output.copy(); }
    public int getProcessTime() { return processTime; }
    public int getEnergyPerTick() { return energyPerTick; }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return input.test(container.getItem(0));
    }

    @Override
    public ItemStack assemble(SimpleContainer container, net.minecraft.core.RegistryAccess access) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
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
        return ModRecipeSerializers.PULVERIZING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.PULVERIZING;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(input);
        return list;
    }

    @Override
    public boolean isSpecial() {
        // Machine-only recipe; keep it out of vanilla recipe book categories.
        return true;
    }

    public static class Serializer implements RecipeSerializer<PulverizerRecipe> {
        @Override
        public PulverizerRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient input = Ingredient.fromJson(GsonHelper.getNonNull(json, "input"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
            int processTime = GsonHelper.getAsInt(json, "process_time", 120);
            int energyPerTick = GsonHelper.getAsInt(json, "energy_per_tick", 20);
            return new PulverizerRecipe(id, input, output, processTime, energyPerTick);
        }

        @Override
        public PulverizerRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            Ingredient input = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            int processTime = buf.readVarInt();
            int energyPerTick = buf.readVarInt();
            return new PulverizerRecipe(id, input, output, processTime, energyPerTick);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, PulverizerRecipe recipe) {
            recipe.input.toNetwork(buf);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.processTime);
            buf.writeVarInt(recipe.energyPerTick);
        }
    }
}
