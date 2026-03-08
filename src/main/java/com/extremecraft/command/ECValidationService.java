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
import net.minecraft.util.GsonHelper;
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
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final String VALIDATION_LOG_PREFIX = "[ExtremeCraft Validation]";

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
            validateMachineDefinitionFiles(resourcesRoot.get(), reporter);
            validateDatapackLayout(resourcesRoot.get(), reporter);
        }

        validateRecipeRuntime(server, reporter);
        validateRegistryEntries(reporter);
        reporter.flushRepeatedWarnings();

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
        Set<String> blockRegistryPaths = new LinkedHashSet<>();
        Set<String> itemRegistryPaths = new LinkedHashSet<>();

        for (ResourceLocation id : ForgeRegistries.BLOCKS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            String path = id.getPath();
            blockRegistryPaths.add(path);
            Path blockstate = assetsRoot.resolve("blockstates").resolve(path + ".json");
            if (!Files.exists(blockstate)) {
                reporter.warn("Missing blockstate for " + id + " (file: " + relativeToResources(resourcesRoot, blockstate) + ")");
            }

            Path blockModel = assetsRoot.resolve("models").resolve("block").resolve(path + ".json");
            if (!Files.exists(blockModel)) {
                reporter.warn("Missing block model for " + id + " (file: " + relativeToResources(resourcesRoot, blockModel) + ")");
            } else {
                validateBlockModelTextureReferences(resourcesRoot, assetsRoot, id, blockModel, reporter);
            }

            Path lootTable = resourcesRoot
                    .resolve("data")
                    .resolve(ECConstants.MODID)
                    .resolve("loot_tables")
                    .resolve("blocks")
                    .resolve(path + ".json");
            if (!Files.exists(lootTable)) {
                reporter.warn("Missing block loot table for " + id + " (file: " + relativeToResources(resourcesRoot, lootTable) + ")");
            }

            String key = "block." + ECConstants.MODID + "." + path;
            if (!langKeys.contains(key)) {
                reporter.warn("Missing lang key for " + id + " (key: " + key + ")");
            }

            validateBlockstateModelReferences(resourcesRoot, assetsRoot, id, blockstate, reporter);
        }

        for (ResourceLocation id : ForgeRegistries.ITEMS.getKeys()) {
            if (!ECConstants.MODID.equals(id.getNamespace())) {
                continue;
            }

            String path = id.getPath();
            itemRegistryPaths.add(path);
            Path itemModel = assetsRoot.resolve("models").resolve("item").resolve(path + ".json");
            if (!Files.exists(itemModel)) {
                reporter.warn("Missing item model for " + id + " (file: " + relativeToResources(resourcesRoot, itemModel) + ")");
            }

            String key = "item." + ECConstants.MODID + "." + path;
            if (!langKeys.contains(key)) {
                reporter.warn("Missing lang key for " + id + " (key: " + key + ")");
            }
        }

        validateArmorLayerTextures(resourcesRoot, assetsRoot, reporter);
        validateModelTextureReferences(resourcesRoot, assetsRoot, reporter);
        validateAbilityIcons(resourcesRoot, assetsRoot, reporter);
        validateOrphanedTopLevelResources(resourcesRoot, assetsRoot, blockRegistryPaths, itemRegistryPaths, reporter);
    }

    private static void validateBlockModelTextureReferences(Path resourcesRoot,
                                                            Path assetsRoot,
                                                            ResourceLocation blockId,
                                                            Path blockModelPath,
                                                            ValidationReporter reporter) {
        try {
            JsonObject json = GSON.fromJson(Files.readString(blockModelPath, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null || !json.has("textures") || !json.get("textures").isJsonObject()) {
                return;
            }

            JsonObject textures = json.getAsJsonObject("textures");
            for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }

                String raw = entry.getValue().getAsString().trim();
                if (raw.isEmpty() || raw.startsWith("#") || raw.startsWith("minecraft:")) {
                    continue;
                }

                String texturePath = raw;
                String textureResource = ECConstants.MODID + ":" + texturePath;

                if (raw.contains(":")) {
                    ResourceLocation rl = ResourceLocation.tryParse(raw);
                    if (rl == null) {
                        reporter.warn("Malformed block texture reference '" + raw + "' in model "
                                + relativeToResources(resourcesRoot, blockModelPath)
                                + " (block: " + blockId + ")");
                        continue;
                    }

                    if (!ECConstants.MODID.equals(rl.getNamespace())) {
                        continue;
                    }

                    texturePath = rl.getPath();
                    textureResource = rl.toString();
                }

                Path textureFile = assetsRoot.resolve("textures").resolve(texturePath + ".png");
                if (!Files.exists(textureFile)) {
                    reporter.warn("Missing block texture for " + blockId
                            + ": " + textureResource
                            + " (model: " + relativeToResources(resourcesRoot, blockModelPath)
                            + ", expected file: " + relativeToResources(resourcesRoot, textureFile) + ")");
                }
            }
        } catch (Exception ex) {
            reporter.warn("Invalid block model JSON for " + blockId + " (file: "
                    + relativeToResources(resourcesRoot, blockModelPath) + "): " + ex.getMessage());
        }
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
                materialHasLeggings.merge(materialId, hasLeggings, (existing, incoming) -> existing || incoming);
            }
        }

        for (String materialId : materialIds) {
            Path layer1 = armorRoot.resolve(materialId + "_layer_1.png");
            if (!Files.exists(layer1)) {
                reporter.warn("Missing armor texture: " + ECConstants.MODID + ":" + materialId + "_layer_1"
                        + " (file: " + relativeToResources(resourcesRoot, layer1) + ")");
            }

            if (materialHasLeggings.getOrDefault(materialId, false)) {
                Path layer2 = armorRoot.resolve(materialId + "_layer_2.png");
                if (!Files.exists(layer2)) {
                    reporter.warn("Missing armor texture: " + ECConstants.MODID + ":" + materialId + "_layer_2"
                            + " (file: " + relativeToResources(resourcesRoot, layer2) + ")");
                }
            }
        }
    }

    private static void validateAbilityIcons(Path resourcesRoot, Path assetsRoot, ValidationReporter reporter) {
        Path dataRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID);
        List<Path> abilityRoots = List.of(dataRoot.resolve("abilities"), dataRoot.resolve("abilities_platform"));

        for (Path abilityRoot : abilityRoots) {
            if (!Files.isDirectory(abilityRoot)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(abilityRoot)) {
                files.filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> validateAbilityIconFile(resourcesRoot, assetsRoot, path, reporter));
            } catch (IOException ex) {
                reporter.warn("Failed to scan ability icon definitions in "
                        + relativeToResources(resourcesRoot, abilityRoot) + ": " + ex.getMessage());
            }
        }
    }

    private static void validateAbilityIconFile(Path resourcesRoot,
                                                Path assetsRoot,
                                                Path abilityFile,
                                                ValidationReporter reporter) {
        try {
            JsonObject json = GSON.fromJson(Files.readString(abilityFile, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                return;
            }

            String fallbackId = stripJsonExtension(abilityFile.getFileName().toString());
            String abilityId = GsonHelper.getAsString(json, "id", fallbackId).trim().toLowerCase();
            if (abilityId.isBlank()) {
                reporter.warn("Ability definition has blank id (file: " + relativeToResources(resourcesRoot, abilityFile) + ")");
                return;
            }

            String iconRef = GsonHelper.getAsString(json, "icon", "").trim();
            if (!iconRef.isBlank()) {
                validateNamedTextureReference(resourcesRoot, assetsRoot, abilityId, abilityFile, "icon", iconRef, reporter);
                return;
            }

            Path defaultIcon = assetsRoot.resolve("textures").resolve("gui").resolve("abilities").resolve(abilityId + ".png");
            Path spellIcon = assetsRoot.resolve("textures").resolve("gui").resolve("spells").resolve(abilityId + ".png");
            if (!Files.exists(defaultIcon) && !Files.exists(spellIcon)) {
                reporter.warn("Missing ability icon: " + ECConstants.MODID + ":" + abilityId
                        + " (ability file: " + relativeToResources(resourcesRoot, abilityFile)
                        + ", expected one of: " + relativeToResources(resourcesRoot, defaultIcon)
                        + " or " + relativeToResources(resourcesRoot, spellIcon) + ")");
            }
        } catch (Exception ex) {
            reporter.warn("Invalid ability definition JSON " + relativeToResources(resourcesRoot, abilityFile) + ": " + ex.getMessage());
        }
    }

    private static void validateOrphanedTopLevelResources(Path resourcesRoot,
                                                           Path assetsRoot,
                                                           Set<String> blockRegistryPaths,
                                                           Set<String> itemRegistryPaths,
                                                           ValidationReporter reporter) {
        warnOrphanedJsonFiles(
                resourcesRoot,
                assetsRoot.resolve("models").resolve("item"),
                itemRegistryPaths,
                "Orphaned item model (no matching item registry id)",
                reporter
        );
        warnOrphanedJsonFiles(
                resourcesRoot,
                assetsRoot.resolve("models").resolve("block"),
                blockRegistryPaths,
                "Orphaned block model (no matching block registry id)",
                reporter
        );
        warnOrphanedJsonFiles(
                resourcesRoot,
                assetsRoot.resolve("blockstates"),
                blockRegistryPaths,
                "Orphaned blockstate (no matching block registry id)",
                reporter
        );

        Path lootBlocks = resourcesRoot.resolve("data").resolve(ECConstants.MODID).resolve("loot_tables").resolve("blocks");
        warnOrphanedJsonFiles(
                resourcesRoot,
                lootBlocks,
                blockRegistryPaths,
                "Orphaned block loot table (no matching block registry id)",
                reporter
        );
    }

    private static void warnOrphanedJsonFiles(Path resourcesRoot,
                                              Path directory,
                                              Set<String> registryPaths,
                                              String messagePrefix,
                                              ValidationReporter reporter) {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                if (!Files.isRegularFile(file)) {
                    continue;
                }

                String stem = stripJsonExtension(file.getFileName().toString());
                if (stem.isBlank() || registryPaths.contains(stem)) {
                    continue;
                }

                reporter.warn(messagePrefix + ": " + ECConstants.MODID + ":" + stem
                        + " (file: " + relativeToResources(resourcesRoot, file) + ")");
            }
        } catch (IOException ex) {
            reporter.warn("Failed to scan directory " + relativeToResources(resourcesRoot, directory) + ": " + ex.getMessage());
        }
    }

    private static void validateBlockstateModelReferences(Path resourcesRoot,
                                                          Path assetsRoot,
                                                          ResourceLocation blockId,
                                                          Path blockstatePath,
                                                          ValidationReporter reporter) {
        if (!Files.exists(blockstatePath)) {
            return;
        }

        try {
            JsonObject json = GSON.fromJson(Files.readString(blockstatePath, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                return;
            }

            collectModelRefs(json, resourcesRoot, assetsRoot, blockId, blockstatePath, reporter);
        } catch (Exception ex) {
            reporter.warn("Invalid blockstate JSON for " + blockId + " (file: "
                    + relativeToResources(resourcesRoot, blockstatePath) + "): " + ex.getMessage());
        }
    }

    private static void collectModelRefs(JsonElement element,
                                         Path resourcesRoot,
                                         Path assetsRoot,
                                         ResourceLocation blockId,
                                         Path blockstatePath,
                                         ValidationReporter reporter) {
        if (element == null || element.isJsonNull()) {
            return;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            if (object.has("model") && object.get("model").isJsonPrimitive()) {
                String modelRef = object.get("model").getAsString().trim();
                if (modelRef.startsWith(ECConstants.MODID + ":")) {
                    modelRef = modelRef.substring((ECConstants.MODID + ":").length());
                }

                if (!modelRef.isBlank() && !modelRef.startsWith("minecraft:")) {
                    Path modelPath = assetsRoot.resolve("models").resolve(modelRef + ".json");
                    if (!Files.exists(modelPath)) {
                        reporter.warn("Blockstate for " + blockId + " references missing model "
                                + ECConstants.MODID + ":" + modelRef
                                + " (blockstate: " + relativeToResources(resourcesRoot, blockstatePath)
                                + ", model file: " + relativeToResources(resourcesRoot, modelPath) + ")");
                    }
                }
            }

            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collectModelRefs(entry.getValue(), resourcesRoot, assetsRoot, blockId, blockstatePath, reporter);
            }
            return;
        }

        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child ->
                    collectModelRefs(child, resourcesRoot, assetsRoot, blockId, blockstatePath, reporter));
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


    private static void validateMachineDefinitionFiles(Path resourcesRoot, ValidationReporter reporter) {
        Path machineRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID).resolve("machines");
        if (!Files.isDirectory(machineRoot)) {
            return;
        }

        Path assetsRoot = resourcesRoot.resolve("assets").resolve(ECConstants.MODID);
        Path recipesRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID).resolve("recipes");

        try (Stream<Path> files = Files.walk(machineRoot)) {
            files.filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> validateMachineDefinitionFile(resourcesRoot, assetsRoot, recipesRoot, path, reporter));
        } catch (IOException ex) {
            reporter.warn("Failed to scan machine definitions: " + ex.getMessage());
        }
    }

    private static void validateMachineDefinitionFile(Path resourcesRoot,
                                                      Path assetsRoot,
                                                      Path recipesRoot,
                                                      Path machineFile,
                                                      ValidationReporter reporter) {
        try {
            JsonObject json = GSON.fromJson(Files.readString(machineFile, StandardCharsets.UTF_8), JsonObject.class);
            if (json == null) {
                reporter.warn("Empty machine definition file: " + relativeToResources(resourcesRoot, machineFile));
                return;
            }

            String fallbackId = stripJsonExtension(machineFile.getFileName().toString());
            String machineId = GsonHelper.getAsString(json, "id", fallbackId).trim().toLowerCase();
            if (machineId.isBlank()) {
                reporter.warn("Machine definition has blank id (file: " + relativeToResources(resourcesRoot, machineFile) + ")");
                return;
            }

            Path blockstatePath = assetsRoot.resolve("blockstates").resolve(machineId + ".json");
            if (!Files.exists(blockstatePath)) {
                reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                        + " missing blockstate (file: " + relativeToResources(resourcesRoot, blockstatePath)
                        + ", source: " + relativeToResources(resourcesRoot, machineFile) + ")");
            }

            Path blockModelPath = assetsRoot.resolve("models").resolve("block").resolve(machineId + ".json");
            if (!Files.exists(blockModelPath)) {
                reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                        + " missing block model (file: " + relativeToResources(resourcesRoot, blockModelPath)
                        + ", source: " + relativeToResources(resourcesRoot, machineFile) + ")");
            }

            Path itemModelPath = assetsRoot.resolve("models").resolve("item").resolve(machineId + ".json");
            if (!Files.exists(itemModelPath)) {
                reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                        + " missing item model (file: " + relativeToResources(resourcesRoot, itemModelPath)
                        + ", source: " + relativeToResources(resourcesRoot, machineFile) + ")");
            }

            validateMachineAssetReferences(resourcesRoot, assetsRoot, machineId, machineFile, json, reporter);

            String recipesRef = GsonHelper.getAsString(json, "recipes", "").trim().toLowerCase();
            if (!recipesRef.isBlank()) {
                ResourceLocation recipeId = parseRecipeId(recipesRef);
                if (recipeId == null) {
                    reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                            + " has invalid recipe reference '" + recipesRef + "'"
                            + " (source: " + relativeToResources(resourcesRoot, machineFile) + ")");
                } else if (ECConstants.MODID.equals(recipeId.getNamespace())) {
                    Path recipePath = recipesRoot.resolve(recipeId.getPath() + ".json");
                    if (!Files.exists(recipePath)) {
                        reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                                + " references missing recipe " + recipeId
                                + " (expected file: " + relativeToResources(resourcesRoot, recipePath)
                                + ", source: " + relativeToResources(resourcesRoot, machineFile) + ")");
                    }
                }
            }
        } catch (Exception ex) {
            reporter.warn("Invalid machine definition JSON " + relativeToResources(resourcesRoot, machineFile) + ": " + ex.getMessage());
        }
    }

    private static void validateMachineAssetReferences(Path resourcesRoot,
                                                       Path assetsRoot,
                                                       String machineId,
                                                       Path machineFile,
                                                       JsonObject machineJson,
                                                       ValidationReporter reporter) {
        validateMachineModelReference(resourcesRoot, assetsRoot, machineId, machineFile, machineJson, "model", reporter);
        validateMachineModelReference(resourcesRoot, assetsRoot, machineId, machineFile, machineJson, "block_model", reporter);
        validateMachineModelReference(resourcesRoot, assetsRoot, machineId, machineFile, machineJson, "item_model", reporter);

        validateNamedTextureReference(resourcesRoot, assetsRoot, machineId, machineFile, "texture", GsonHelper.getAsString(machineJson, "texture", "").trim(), reporter);
        validateNamedTextureReference(resourcesRoot, assetsRoot, machineId, machineFile, "icon", GsonHelper.getAsString(machineJson, "icon", "").trim(), reporter);
        validateNamedTextureReference(resourcesRoot, assetsRoot, machineId, machineFile, "gui_texture", GsonHelper.getAsString(machineJson, "gui_texture", "").trim(), reporter);
    }

    private static void validateMachineModelReference(Path resourcesRoot,
                                                      Path assetsRoot,
                                                      String machineId,
                                                      Path machineFile,
                                                      JsonObject machineJson,
                                                      String key,
                                                      ValidationReporter reporter) {
        String modelRef = GsonHelper.getAsString(machineJson, key, "").trim();
        if (modelRef.isBlank()) {
            return;
        }

        ParsedResourceRef parsed = parseResourceRef(modelRef);
        if (parsed.isMalformed()) {
            reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                    + " has malformed model reference '" + modelRef + "' in key '" + key + "'"
                    + " (source: " + relativeToResources(resourcesRoot, machineFile) + ")");
            return;
        }

        if (!parsed.isLocalMod()) {
            return;
        }

        String path = stripJsonExtension(parsed.path());
        List<Path> candidates = new ArrayList<>();
        if ("block_model".equals(key)) {
            candidates.add(assetsRoot.resolve("models").resolve("block").resolve(path + ".json"));
            if (path.startsWith("block/")) {
                candidates.add(assetsRoot.resolve("models").resolve(path + ".json"));
            }
        } else if ("item_model".equals(key)) {
            candidates.add(assetsRoot.resolve("models").resolve("item").resolve(path + ".json"));
            if (path.startsWith("item/")) {
                candidates.add(assetsRoot.resolve("models").resolve(path + ".json"));
            }
        } else {
            candidates.add(assetsRoot.resolve("models").resolve(path + ".json"));
            candidates.add(assetsRoot.resolve("models").resolve("block").resolve(path + ".json"));
            candidates.add(assetsRoot.resolve("models").resolve("item").resolve(path + ".json"));
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return;
            }
        }

        Path expected = candidates.get(0);
        reporter.warn("Machine " + ECConstants.MODID + ":" + machineId
                + " references missing model " + parsed.resourceLocation()
                + " (key: " + key
                + ", source: " + relativeToResources(resourcesRoot, machineFile)
                + ", expected file: " + relativeToResources(resourcesRoot, expected) + ")");
    }

    private static void validateNamedTextureReference(Path resourcesRoot,
                                                      Path assetsRoot,
                                                      String ownerId,
                                                      Path sourceFile,
                                                      String key,
                                                      String textureRef,
                                                      ValidationReporter reporter) {
        if (textureRef == null || textureRef.isBlank()) {
            return;
        }

        ParsedResourceRef parsed = parseResourceRef(textureRef);
        if (parsed.isMalformed()) {
            reporter.warn("Malformed texture reference '" + textureRef + "' in key '" + key + "'"
                    + " (id: " + ECConstants.MODID + ":" + ownerId
                    + ", source: " + relativeToResources(resourcesRoot, sourceFile) + ")");
            return;
        }

        if (!parsed.isLocalMod()) {
            return;
        }

        String texturePath = parsed.path();
        if (texturePath.startsWith("textures/")) {
            texturePath = texturePath.substring("textures/".length());
        }
        if (texturePath.endsWith(".png")) {
            texturePath = texturePath.substring(0, texturePath.length() - 4);
        }

        Path textureFile = assetsRoot.resolve("textures").resolve(texturePath + ".png");
        if (!Files.exists(textureFile)) {
            reporter.warn("Missing texture asset " + parsed.resourceLocation()
                    + " for " + ECConstants.MODID + ":" + ownerId
                    + " (key: " + key
                    + ", source: " + relativeToResources(resourcesRoot, sourceFile)
                    + ", expected file: " + relativeToResources(resourcesRoot, textureFile) + ")");
        }
    }

    private static String stripJsonExtension(String fileName) {
        return fileName.endsWith(".json") ? fileName.substring(0, fileName.length() - 5) : fileName;
    }

    private static ResourceLocation parseRecipeId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.contains(":") ? value : ECConstants.MODID + ":" + value;
        return ResourceLocation.tryParse(normalized);
    }


    private static void validateDatapackLayout(Path resourcesRoot, ValidationReporter reporter) {
        Path dataRoot = resourcesRoot.resolve("data").resolve(ECConstants.MODID);
        // TODO(alpha): keep this compatibility warning until legacy datapack roots are fully retired.
        warnIfLegacyDirectoryPopulated(dataRoot, "skill_trees", "skilltrees", reporter);
        warnIfLegacyDirectoryPopulated(dataRoot, "machines", "machine", reporter);
    }

    private static void warnIfLegacyDirectoryPopulated(Path dataRoot,
                                                       String canonicalDir,
                                                       String legacyDir,
                                                       ValidationReporter reporter) {
        Path canonical = dataRoot.resolve(canonicalDir);
        Path legacy = dataRoot.resolve(legacyDir);
        if (!Files.isDirectory(legacy)) {
            return;
        }

        long legacyJson = countJsonFiles(legacy);
        if (legacyJson <= 0) {
            return;
        }

        long canonicalJson = countJsonFiles(canonical);
        reporter.warn("Legacy datapack directory detected: data/" + ECConstants.MODID + "/" + legacyDir
                + " (json=" + legacyJson + "). Canonical directory is data/" + ECConstants.MODID + "/" + canonicalDir
                + " (json=" + canonicalJson + "). Keep content in the canonical path to avoid reload drift.");
    }

    private static long countJsonFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return 0L;
        }

        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".json")).count();
        } catch (IOException ignored) {
            return 0L;
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

        if (!("item".equals(key) || "result".equals(key) || "tag".equals(key))) {
            return;
        }

        String value = element.getAsString();
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            reporter.warn("Invalid resource location in recipe " + relativeToResources(resourcesRoot, recipePath)
                    + " (key: " + key + "): " + value);
            return;
        }

        if ("tag".equals(key)) {
            validateRecipeTagReference(resourcesRoot, recipePath, id, reporter);
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

    private static void validateRecipeTagReference(Path resourcesRoot,
                                                   Path recipePath,
                                                   ResourceLocation tagId,
                                                   ValidationReporter reporter) {
        if (!ECConstants.MODID.equals(tagId.getNamespace())) {
            return;
        }

        Path itemsTag = resourcesRoot.resolve("data").resolve(ECConstants.MODID)
                .resolve("tags").resolve("items").resolve(tagId.getPath() + ".json");
        Path blocksTag = resourcesRoot.resolve("data").resolve(ECConstants.MODID)
                .resolve("tags").resolve("blocks").resolve(tagId.getPath() + ".json");

        if (!Files.exists(itemsTag) && !Files.exists(blocksTag)) {
            reporter.warn("Unknown tag in recipe " + relativeToResources(resourcesRoot, recipePath)
                    + ": " + tagId
                    + " (expected one of: " + relativeToResources(resourcesRoot, itemsTag)
                    + " or " + relativeToResources(resourcesRoot, blocksTag) + ")");
        }
    }

    private static ParsedResourceRef parseResourceRef(String value) {
        if (value == null) {
            return ParsedResourceRef.malformedRef();
        }

        String normalized = value.trim();
        if (normalized.isBlank()) {
            return ParsedResourceRef.malformedRef();
        }

        if (normalized.contains(":")) {
            ResourceLocation rl = ResourceLocation.tryParse(normalized);
            if (rl == null) {
                return ParsedResourceRef.malformedRef();
            }
            return new ParsedResourceRef(rl.getNamespace(), rl.getPath(), false);
        }

        return new ParsedResourceRef(ECConstants.MODID, normalized, false);
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
        private final Map<String, Integer> warningCounts = new LinkedHashMap<>();

        private ValidationReporter(CommandSourceStack source) {
            this.source = source;
        }

        private void warn(String message) {
            int count = warningCounts.getOrDefault(message, 0) + 1;
            warningCounts.put(message, count);
            if (count > 1) {
                return;
            }

            warnings++;
            LOGGER.warn("{} {}", VALIDATION_LOG_PREFIX, message);
            if (emitted < MAX_CHAT_WARNINGS) {
                emitted++;
                source.sendFailure(Component.literal("[ecvalidate] " + message));
            }
        }

        private void flushRepeatedWarnings() {
            for (Map.Entry<String, Integer> entry : warningCounts.entrySet()) {
                int count = entry.getValue();
                if (count <= 1) {
                    continue;
                }

                String summary = "Repeated warning (" + count + "x): " + entry.getKey();
                LOGGER.warn("{} {}", VALIDATION_LOG_PREFIX, summary);
                if (emitted < MAX_CHAT_WARNINGS) {
                    emitted++;
                    source.sendFailure(Component.literal("[ecvalidate] " + summary));
                }
            }
        }

        private int warningCount() {
            return warnings;
        }

        private int omittedCount() {
            return Math.max(0, warnings - emitted);
        }
    }

    private record ParsedResourceRef(String namespace, String path, boolean malformed) {
        private static ParsedResourceRef malformedRef() {
            return new ParsedResourceRef("", "", true);
        }

        private boolean isMalformed() {
            return malformed;
        }

        private boolean isLocalMod() {
            return !malformed && ECConstants.MODID.equals(namespace);
        }

        private String resourceLocation() {
            return namespace + ":" + path;
        }
    }
}

