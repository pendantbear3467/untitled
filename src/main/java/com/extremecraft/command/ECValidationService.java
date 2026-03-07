package com.extremecraft.command;

import com.extremecraft.core.ECConstants;
import com.extremecraft.machine.recipe.ModTechRecipeTypes;
import com.extremecraft.machines.recipe.ModRecipeTypes;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Runtime validator for common content issues in development.
 */
public final class ECValidationService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new Gson();
    private static final Set<String> CRAFTING_CATEGORIES = Set.of("building", "redstone", "equipment", "misc");
    private static final Set<String> COOKING_CATEGORIES = Set.of("food", "blocks", "misc");

    private ECValidationService() {
    }

    public static int run(CommandSourceStack source) {
        ValidationReporter reporter = new ValidationReporter(source);
        MinecraftServer server = source.getServer();

        source.sendSuccess(() -> Component.literal("[ecvalidate] Starting runtime validation..."), false);

        Optional<Path> resourcesRoot = resolveResourcesRoot();
        if (resourcesRoot.isEmpty()) {
            reporter.warn("Could not locate resources root (expected src/main/resources or build/resources/main relative to game dir).");
        } else {
            validateAssets(resourcesRoot.get(), reporter);
            validateRecipeJson(resourcesRoot.get(), reporter);
        }

        validateRecipeRuntime(server, reporter);
        validateRegistryEntries(reporter);

        int warnings = reporter.warningCount();
        int omitted = reporter.omittedCount();
        if (omitted > 0) {
            source.sendSuccess(() -> Component.literal("[ecvalidate] ...and " + omitted + " additional warning(s) (see log)."), false);
        }

        int finalWarnings = warnings;
        source.sendSuccess(() -> Component.literal("[ecvalidate] Completed. warnings=" + finalWarnings), true);
        return warnings == 0 ? 1 : 0;
    }

    private static void validateRecipeRuntime(MinecraftServer server, ValidationReporter reporter) {
        int machineRecipes = server.getRecipeManager().getAllRecipesFor(ModTechRecipeTypes.MACHINE_PROCESSING).size();
        if (machineRecipes <= 0) {
            reporter.warn("No machine processing recipes loaded for type extremecraft:machine_processing.");
        }

        int pulverizingRecipes = server.getRecipeManager().getAllRecipesFor(ModRecipeTypes.PULVERIZING).size();
        if (pulverizingRecipes <= 0) {
            reporter.warn("No pulverizing recipes loaded for type extremecraft:pulverizing.");
        }
    }

    private static void validateRegistryEntries(ValidationReporter reporter) {
        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            Block block = ForgeRegistries.BLOCKS.getValue(id);
            if (block == null) {
                reporter.warn("Block registry entry resolves to null: " + id);
            }
            if (id.getPath().isBlank()) {
                reporter.warn("Block has blank registry path: " + id);
            }
        }

        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null) {
                reporter.warn("Item registry entry resolves to null: " + id);
            }
            if (id.getPath().isBlank()) {
                reporter.warn("Item has blank registry path: " + id);
            }
        }
    }

    private static void validateAssets(Path resourcesRoot, ValidationReporter reporter) {
        Path assetsRoot = resourcesRoot.resolve("assets").resolve(ECConstants.MODID);
        Path langFile = assetsRoot.resolve("lang").resolve("en_us.json");

        Set<String> langKeys = loadLangKeys(langFile, reporter);

        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            String path = id.getPath();
            Path blockstate = assetsRoot.resolve("blockstates").resolve(path + ".json");
            if (!Files.exists(blockstate)) {
                reporter.warn("Missing blockstate file: " + relativeToResources(resourcesRoot, blockstate));
            }

            Path lootTable = resourcesRoot
                    .resolve("data")
                    .resolve(ECConstants.MODID)
                    .resolve("loot_tables")
                    .resolve("blocks")
                    .resolve(path + ".json");
            if (!Files.exists(lootTable)) {
                reporter.warn("Missing block loot table: " + relativeToResources(resourcesRoot, lootTable));
            }

            String key = "block." + ECConstants.MODID + "." + path;
            if (!langKeys.contains(key)) {
                reporter.warn("Missing lang key: " + key);
            }
        }

        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            String path = id.getPath();
            Path itemModel = assetsRoot.resolve("models").resolve("item").resolve(path + ".json");
            if (!Files.exists(itemModel)) {
                reporter.warn("Missing item model file: " + relativeToResources(resourcesRoot, itemModel));
            }

            String key = "item." + ECConstants.MODID + "." + path;
            if (!langKeys.contains(key)) {
                reporter.warn("Missing lang key: " + key);
            }
        }

        validateArmorLayerTextures(resourcesRoot, assetsRoot, reporter);
        validateModelTextureReferences(resourcesRoot, assetsRoot, reporter);
    }

    private static void validateArmorLayerTextures(Path resourcesRoot, Path assetsRoot, ValidationReporter reporter) {
        Path armorRoot = assetsRoot.resolve("textures").resolve("models").resolve("armor");
        Set<String> materialIds = new LinkedHashSet<>();
        Map<String, Boolean> materialHasLeggings = new HashMap<>();

        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (!(item instanceof ArmorItem armorItem)) {
                continue;
            }

            String materialId = armorTextureBaseName(armorItem.getMaterial());
            if (!materialId.isEmpty()) {
                materialIds.add(materialId);
                boolean hasLeggings = armorItem.getType() == ArmorItem.Type.LEGGINGS;
                materialHasLeggings.merge(materialId, hasLeggings, Boolean::logicalOr);
            }
        }

        for (String materialId : materialIds) {
            Path layer1 = armorRoot.resolve(materialId + "_layer_1.png");
            if (!Files.exists(layer1)) {
                reporter.warn("Missing armor layer texture: " + relativeToResources(resourcesRoot, layer1));
            }

            if (materialHasLeggings.getOrDefault(materialId, false)) {
                Path layer2 = armorRoot.resolve(materialId + "_layer_2.png");
                if (!Files.exists(layer2)) {
                    reporter.warn("Missing armor layer texture: " + relativeToResources(resourcesRoot, layer2));
                }
            }
        }
    }

    private static String armorTextureBaseName(ArmorMaterial material) {
        String name = material.getName();
        if (name == null || name.isBlank()) {
            return "";
        }

        int separator = name.indexOf(':');
        return separator >= 0 ? name.substring(separator + 1) : name;
    }

    private static Set<String> loadLangKeys(Path langFile, ValidationReporter reporter) {
        if (!Files.exists(langFile)) {
            reporter.warn("Missing language file: " + langFile);
            return Set.of();
        }

        try {
            JsonObject json = GSON.fromJson(Files.readString(langFile, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                reporter.warn("Language file is empty or invalid JSON: " + langFile);
                return Set.of();
            }

            Set<String> keys = new HashSet<>();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                keys.add(entry.getKey());
            }
            return keys;
        } catch (IOException ex) {
            reporter.warn("Failed to read language file " + langFile + ": " + ex.getMessage());
            return Set.of();
        }
    }

    private static void validateModelTextureReferences(Path resourcesRoot, Path assetsRoot, ValidationReporter reporter) {
        Path modelsRoot = assetsRoot.resolve("models");
        if (!Files.isDirectory(modelsRoot)) {
            reporter.warn("Missing model root: " + relativeToResources(resourcesRoot, modelsRoot));
            return;
        }

        try (Stream<Path> files = Files.walk(modelsRoot)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(modelPath -> validateModelFile(resourcesRoot, assetsRoot, modelPath, reporter));
        } catch (IOException ex) {
            reporter.warn("Failed to scan model files: " + ex.getMessage());
        }
    }

    private static void validateModelFile(Path resourcesRoot, Path assetsRoot, Path modelPath, ValidationReporter reporter) {
        try {
            JsonObject json = GSON.fromJson(Files.readString(modelPath, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null || !json.has("textures") || !json.get("textures").isJsonObject()) {
                return;
            }

            JsonObject textures = json.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> textureEntry : textures.entrySet()) {
                if (!textureEntry.getValue().isJsonPrimitive()) {
                    continue;
                }

                String value = textureEntry.getValue().getAsString();
                if (value.startsWith("#") || !value.startsWith(ECConstants.MODID + ":")) {
                    continue;
                }

                String path = value.substring((ECConstants.MODID + ":").length());
                Path texturePath = assetsRoot.resolve("textures").resolve(path + ".png");
                if (!Files.exists(texturePath)) {
                    reporter.warn("Missing texture referenced by model "
                            + relativeToResources(resourcesRoot, modelPath)
                            + " -> " + relativeToResources(resourcesRoot, texturePath));
                }
            }
        } catch (Exception ex) {
            reporter.warn("Invalid model JSON " + relativeToResources(resourcesRoot, modelPath) + ": " + ex.getMessage());
        }
    }

    private static void validateRecipeJson(Path resourcesRoot, ValidationReporter reporter) {
        Path recipesRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID).resolve("recipes");
        if (!Files.isDirectory(recipesRoot)) {
            reporter.warn("Missing recipe directory: " + relativeToResources(resourcesRoot, recipesRoot));
            return;
        }

        try (Stream<Path> files = Files.walk(recipesRoot)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> validateRecipeFile(resourcesRoot, path, reporter));
        } catch (IOException ex) {
            reporter.warn("Failed to scan recipe files: " + ex.getMessage());
        }
    }

    private static void validateRecipeFile(Path resourcesRoot, Path recipePath, ValidationReporter reporter) {
        try {
            JsonElement root = GSON.fromJson(Files.readString(recipePath, StandardCharsets.UTF_8), JsonElement.class);
            if (root == null) {
                reporter.warn("Empty recipe JSON: " + relativeToResources(resourcesRoot, recipePath));
                return;
            }

            validateRecipeCategory(root, resourcesRoot, recipePath, reporter);

            validateRecipeElement(root, "", resourcesRoot, recipePath, reporter);
        } catch (Exception ex) {
            reporter.warn("Invalid recipe JSON " + relativeToResources(resourcesRoot, recipePath) + ": " + ex.getMessage());
        }
    }

    private static void validateRecipeCategory(JsonElement root, Path resourcesRoot, Path recipePath, ValidationReporter reporter) {
        if (!root.isJsonObject()) {
            return;
        }

        JsonObject object = root.getAsJsonObject();
        String type = stringOrEmpty(object.get("type"));
        if (!type.startsWith("minecraft:")) {
            return;
        }

        String category = stringOrEmpty(object.get("category"));
        if (category.isEmpty()) {
            return;
        }

        String normalized = category.toLowerCase();
        if (isCraftingRecipe(type) && !CRAFTING_CATEGORIES.contains(normalized)) {
            reporter.warn("Invalid crafting recipe category in " + relativeToResources(resourcesRoot, recipePath)
                    + ": '" + category + "' (expected one of " + CRAFTING_CATEGORIES + ")");
            return;
        }

        if (isCookingRecipe(type) && !COOKING_CATEGORIES.contains(normalized)) {
            reporter.warn("Invalid cooking recipe category in " + relativeToResources(resourcesRoot, recipePath)
                    + ": '" + category + "' (expected one of " + COOKING_CATEGORIES + ")");
        }
    }

    private static String stringOrEmpty(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return "";
        }
        return element.getAsString().trim();
    }

    private static boolean isCraftingRecipe(String type) {
        return "minecraft:crafting_shaped".equals(type) || "minecraft:crafting_shapeless".equals(type);
    }

    private static boolean isCookingRecipe(String type) {
        return "minecraft:smelting".equals(type)
                || "minecraft:blasting".equals(type)
                || "minecraft:smoking".equals(type)
                || "minecraft:campfire_cooking".equals(type);
    }

    private static void validateRecipeElement(JsonElement element, String key, Path resourcesRoot, Path recipePath, ValidationReporter reporter) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                validateRecipeElement(entry.getValue(), entry.getKey(), resourcesRoot, recipePath, reporter);
            }
            return;
        }

        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> validateRecipeElement(child, key, resourcesRoot, recipePath, reporter));
            return;
        }

        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return;
        }

        if (!("item".equals(key) || "result".equals(key))) {
            return;
        }

        String value = element.getAsString();
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            reporter.warn("Invalid item id in recipe " + relativeToResources(resourcesRoot, recipePath) + ": " + value);
            return;
        }

        if (!("minecraft".equals(id.getNamespace()) || ECConstants.MODID.equals(id.getNamespace()))) {
            return;
        }

        Item item = ForgeRegistries.ITEMS.getValue(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            reporter.warn("Unknown item id in recipe " + relativeToResources(resourcesRoot, recipePath) + ": " + id);
        }
    }

    private static Optional<Path> resolveResourcesRoot() {
        Path gameDir = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path parent = gameDir.getParent();
        Path[] candidates = new Path[]{
                gameDir.resolve("src").resolve("main").resolve("resources"),
            gameDir.resolve("build").resolve("resources").resolve("main"),
            parent == null ? gameDir.resolve("src").resolve("main").resolve("resources") : parent.resolve("src").resolve("main").resolve("resources"),
            parent == null ? gameDir.resolve("build").resolve("resources").resolve("main") : parent.resolve("build").resolve("resources").resolve("main")
        };

        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    private static String relativeToResources(Path resourcesRoot, Path path) {
        try {
            return resourcesRoot.relativize(path).toString().replace('\\', '/');
        } catch (Exception ignored) {
            return path.toString().replace('\\', '/');
        }
    }

    private static final class ValidationReporter {
        private static final int MAX_CHAT_WARNINGS = 12;

        private final CommandSourceStack source;
        private int warnings;
        private int emitted;

        private ValidationReporter(CommandSourceStack source) {
            this.source = source;
        }

        private void warn(String message) {
            warnings++;
            LOGGER.warn("[ecvalidate] {}", message);
            if (emitted < MAX_CHAT_WARNINGS) {
                emitted++;
                source.sendFailure(Component.literal("[ecvalidate] " + message));
            }
        }

        private int warningCount() {
            return warnings;
        }

        private int omittedCount() {
            return Math.max(0, warnings - emitted);
        }
    }
}
