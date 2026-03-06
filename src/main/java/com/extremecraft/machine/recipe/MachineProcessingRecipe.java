package com.extremecraft.machine.recipe;

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

public class MachineProcessingRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final String machineId;
    private final Ingredient input;
    private final ItemStack output;
    private final int processTime;
    private final int energyPerTick;

    public MachineProcessingRecipe(ResourceLocation id, String machineId, Ingredient input, ItemStack output, int processTime, int energyPerTick) {
        this.id = id;
        this.machineId = machineId;
        this.input = input;
        this.output = output;
        this.processTime = processTime;
        this.energyPerTick = energyPerTick;
    }

    public String machineId() {
        return machineId;
    }

    public ItemStack output() {
        return output.copy();
    }

    public int processTime() {
        return processTime;
    }

    public int energyPerTick() {
        return energyPerTick;
    }

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
        return TechRecipeSerializers.MACHINE_PROCESSING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModTechRecipeTypes.MACHINE_PROCESSING;
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

    public static class Serializer implements RecipeSerializer<MachineProcessingRecipe> {
        @Override
        public MachineProcessingRecipe fromJson(ResourceLocation id, JsonObject json) {
            String machine = GsonHelper.getAsString(json, "machine");
            Ingredient input = Ingredient.fromJson(GsonHelper.getNonNull(json, "input"));
            ItemStack output = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "output"));
            int process = GsonHelper.getAsInt(json, "process_time", 120);
            int ept = GsonHelper.getAsInt(json, "energy_per_tick", 20);
            return new MachineProcessingRecipe(id, machine, input, output, process, ept);
        }

        @Override
        public MachineProcessingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            String machine = buf.readUtf();
            Ingredient input = Ingredient.fromNetwork(buf);
            ItemStack output = buf.readItem();
            int process = buf.readVarInt();
            int ept = buf.readVarInt();
            return new MachineProcessingRecipe(id, machine, input, output, process, ept);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, MachineProcessingRecipe recipe) {
            buf.writeUtf(recipe.machineId);
            recipe.input.toNetwork(buf);
            buf.writeItem(recipe.output);
            buf.writeVarInt(recipe.processTime);
            buf.writeVarInt(recipe.energyPerTick);
        }
    }
}
