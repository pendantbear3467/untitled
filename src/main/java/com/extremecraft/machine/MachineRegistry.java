package com.extremecraft.machine;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MachineRegistry {
    private static final Map<String, MachineDefinition> MACHINES = new LinkedHashMap<>();
    private static final Map<String, MachineRecipe> RECIPES = new LinkedHashMap<>();
    private static final Map<String, List<MachineRecipe>> RECIPES_BY_MACHINE = new LinkedHashMap<>();

    private static List<MachineDefinition> CACHED_MACHINES = List.of();
    private static List<MachineRecipe> CACHED_RECIPES = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized MachineDefinition getMachine(String id) {
        return id == null ? null : MACHINES.get(normalize(id));
    }

    public static synchronized Collection<MachineDefinition> machines() {
        return CACHED_MACHINES;
    }

    public static synchronized Collection<MachineRecipe> recipes() {
        return CACHED_RECIPES;
    }

    public static synchronized List<MachineRecipe> recipesForMachine(String machineId) {
        if (machineId == null) {
            return List.of();
        }
        return RECIPES_BY_MACHINE.getOrDefault(normalize(machineId), List.of());
    }

    public static synchronized MachineRecipe recipe(String id) {
        return id == null ? null : RECIPES.get(normalize(id));
    }

    private static synchronized void replace(Map<String, MachineDefinition> machines, Map<String, MachineRecipe> recipes) {
        MACHINES.clear();
        MACHINES.putAll(machines);

        RECIPES.clear();
        RECIPES.putAll(recipes);

        RECIPES_BY_MACHINE.clear();
        Map<String, List<MachineRecipe>> byMachine = new LinkedHashMap<>();
        for (MachineRecipe recipe : RECIPES.values()) {
            byMachine.computeIfAbsent(normalize(recipe.machineId()), ignored -> new ArrayList<>()).add(recipe);
        }
        byMachine.forEach((machineId, machineRecipes) -> RECIPES_BY_MACHINE.put(machineId, List.copyOf(machineRecipes)));

        CACHED_MACHINES = List.copyOf(MACHINES.values());
        CACHED_RECIPES = List.copyOf(RECIPES.values());
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        private Loader() {
            super(GSON, "machines");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, MachineDefinition> machines = new LinkedHashMap<>();
            Map<String, MachineRecipe> recipes = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject root = entry.getValue().getAsJsonObject();
                String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase();
                String tier = GsonHelper.getAsString(root, "tier", "basic").trim().toLowerCase();
                int energyPerTick = Math.max(0, GsonHelper.getAsInt(root, "energy_per_tick", 0));
                int processTicks = Math.max(1, GsonHelper.getAsInt(root, "process_ticks", 200));
                int inputSlots = Math.max(1, GsonHelper.getAsInt(root, "input_slots", 1));
                int outputSlots = Math.max(1, GsonHelper.getAsInt(root, "output_slots", 1));

                boolean supportFe = GsonHelper.getAsBoolean(root, "supports_fe", true);
                boolean supportEc = GsonHelper.getAsBoolean(root, "supports_ec", true);

                List<String> recipeIds = List.of();
                if (root.has("recipes") && root.get("recipes").isJsonArray()) {
                    JsonArray recipeArray = root.getAsJsonArray("recipes");
                    ArrayList<String> ids = new ArrayList<>();
                    for (JsonElement recipeId : recipeArray) {
                        String value = recipeId.getAsString().trim().toLowerCase();
                        if (!value.isBlank()) {
                            ids.add(value);
                        }
                    }
                    recipeIds = List.copyOf(ids);
                }

                machines.put(id, new MachineDefinition(id, tier, inputSlots, outputSlots, processTicks, energyPerTick, supportFe, supportEc, recipeIds));

                if (!root.has("machine_recipes") || !root.get("machine_recipes").isJsonArray()) {
                    continue;
                }

                JsonArray rawRecipes = root.getAsJsonArray("machine_recipes");
                for (JsonElement rawRecipe : rawRecipes) {
                    if (!rawRecipe.isJsonObject()) {
                        continue;
                    }

                    JsonObject recipe = rawRecipe.getAsJsonObject();
                    String recipeId = GsonHelper.getAsString(recipe, "id", id + "_default").trim().toLowerCase();
                    int recipeTicks = Math.max(1, GsonHelper.getAsInt(recipe, "process_ticks", processTicks));
                    int recipeEnergy = Math.max(0, GsonHelper.getAsInt(recipe, "energy_cost", recipeTicks * energyPerTick));

                    Map<String, Integer> input = readIngredientMap(recipe, "input");
                    Map<String, Integer> output = readIngredientMap(recipe, "output");
                    recipes.put(recipeId, new MachineRecipe(recipeId, id, input, output, recipeTicks, recipeEnergy));
                }
            }

            ExtremeCraftAPI.machines().forEach(apiMachine -> machines.putIfAbsent(apiMachine.id(),
                    new MachineDefinition(apiMachine.id(), apiMachine.tier(), 1, 1, 200, 20, true, true, List.of())));

            replace(machines, recipes);
        }

        private static Map<String, Integer> readIngredientMap(JsonObject root, String key) {
            Map<String, Integer> map = new LinkedHashMap<>();
            if (!root.has(key) || !root.get(key).isJsonObject()) {
                return Map.copyOf(map);
            }

            JsonObject raw = root.getAsJsonObject(key);
            for (Map.Entry<String, JsonElement> entry : raw.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    map.put(entry.getKey().trim().toLowerCase(), Math.max(1, entry.getValue().getAsInt()));
                }
            }

            return Map.copyOf(map);
        }
    }
}
