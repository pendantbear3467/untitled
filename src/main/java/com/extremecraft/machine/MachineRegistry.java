package com.extremecraft.machine;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Runtime machine catalog backed by datapack reloads and API extension hooks.
 *
 * <p>This registry is read by machine block entities every server tick, so it keeps
 * immutable cached views for fast reads while still allowing full replacement when
 * datapacks reload. API and module integrations append definitions through the
 * registration methods and are merged with datapack content.</p>
 */
public final class MachineRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

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

    /**
     * Extension hook for other modules/mods to register machine definitions at runtime.
     */
    public static synchronized void registerMachineDefinition(MachineDefinition definition) {
        if (definition == null || normalize(definition.id()).isBlank()) {
            return;
        }

        MACHINES.put(normalize(definition.id()), definition);
        CACHED_MACHINES = List.copyOf(MACHINES.values());
    }

    /**
     * Extension hook for other modules/mods to register machine recipes at runtime.
     */
    public static synchronized void registerRecipeDefinition(MachineRecipe recipe) {
        if (recipe == null || normalize(recipe.id()).isBlank() || normalize(recipe.machineId()).isBlank()) {
            return;
        }

        String machineId = normalize(recipe.machineId());
        RECIPES.put(normalize(recipe.id()), recipe);

        List<MachineRecipe> rebuilt = new ArrayList<>(RECIPES_BY_MACHINE.getOrDefault(machineId, List.of()));
        rebuilt.add(recipe);
        RECIPES_BY_MACHINE.put(machineId, List.copyOf(rebuilt));

        CACHED_RECIPES = List.copyOf(RECIPES.values());
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
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
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
                try {
                    if (!entry.getValue().isJsonObject()) {
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    String id = normalize(GsonHelper.getAsString(root, "id", entry.getKey().getPath()));
                    if (id.contains("/")) {
                        id = id.substring(id.lastIndexOf('/') + 1);
                    }
                    if (id.isBlank()) {
                        LOGGER.warn("[Machine] Skipping machine with blank id from {}", entry.getKey());
                        continue;
                    }

                    String tier = normalize(GsonHelper.getAsString(root, "tier", "basic"));
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
                            String value = normalize(recipeId.getAsString());
                            if (!value.isBlank()) {
                                ids.add(value);
                            }
                        }
                        recipeIds = List.copyOf(ids);
                    }

                    MachineDefinition previousMachine = machines.put(id,
                            new MachineDefinition(id, tier, inputSlots, outputSlots, processTicks, energyPerTick, supportFe, supportEc, recipeIds));
                    if (previousMachine != null) {
                        LOGGER.warn("[Machine] Duplicate machine id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }

                    if (!root.has("machine_recipes") || !root.get("machine_recipes").isJsonArray()) {
                        continue;
                    }

                    JsonArray rawRecipes = root.getAsJsonArray("machine_recipes");
                    for (JsonElement rawRecipe : rawRecipes) {
                        try {
                            if (!rawRecipe.isJsonObject()) {
                                continue;
                            }

                            JsonObject recipe = rawRecipe.getAsJsonObject();
                            String recipeId = normalize(GsonHelper.getAsString(recipe, "id", id + "_default"));
                            if (recipeId.isBlank()) {
                                LOGGER.warn("[Machine] Skipping recipe with blank id in {}", entry.getKey());
                                continue;
                            }

                            int recipeTicks = Math.max(1, GsonHelper.getAsInt(recipe, "process_ticks", processTicks));
                            int recipeEnergy = Math.max(0, GsonHelper.getAsInt(recipe, "energy_cost", recipeTicks * energyPerTick));

                            Map<String, Integer> input = readIngredientMap(recipe, "input");
                            Map<String, Integer> output = readIngredientMap(recipe, "output");
                            MachineRecipe previousRecipe = recipes.put(recipeId,
                                    new MachineRecipe(recipeId, id, input, output, recipeTicks, recipeEnergy));
                            if (previousRecipe != null) {
                                LOGGER.warn("[Machine] Duplicate machine recipe id '{}' detected; keeping latest", recipeId);
                            }
                        } catch (RuntimeException recipeEx) {
                            LOGGER.warn("[Machine] Skipping malformed recipe entry in {}: {}", entry.getKey(), recipeEx.getMessage());
                        }
                    }
                } catch (RuntimeException ex) {
                    LOGGER.warn("[Machine] Skipping malformed machine definition {}: {}", entry.getKey(), ex.getMessage());
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
                    map.put(normalize(entry.getKey()), Math.max(1, entry.getValue().getAsInt()));
                }
            }

            return Map.copyOf(map);
        }
    }
}
