package com.extremecraft.gameplay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayStabilityDataTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final Path RESOURCES = ROOT.resolve("src/main/resources");

    @Test
    void oreGenerationAndMiningCoverageIsComplete() throws IOException {
        Set<String> oreBlockIds = oreBlockIds();

        JsonObject pickaxeTag = readJson(RESOURCES.resolve("data/minecraft/tags/blocks/mineable/pickaxe.json"));
        JsonObject stoneTag = readJson(RESOURCES.resolve("data/minecraft/tags/blocks/needs_stone_tool.json"));
        Set<String> pickaxeValues = jsonStringSet(pickaxeTag, "values");
        Set<String> stoneValues = jsonStringSet(stoneTag, "values");

        for (String oreId : oreBlockIds) {
            assertTrue(Files.exists(RESOURCES.resolve("data/extremecraft/worldgen/configured_feature/" + oreId + ".json")),
                    "Missing configured feature for " + oreId);
            assertTrue(Files.exists(RESOURCES.resolve("data/extremecraft/worldgen/placed_feature/" + oreId + ".json")),
                    "Missing placed feature for " + oreId);
            assertTrue(Files.exists(RESOURCES.resolve("data/extremecraft/forge/biome_modifier/add_" + oreId + ".json")),
                    "Missing biome modifier for " + oreId);

            String namespaced = "extremecraft:" + oreId;
            assertTrue(pickaxeValues.contains(namespaced), "Missing pickaxe tag for " + oreId);
            assertTrue(stoneValues.contains(namespaced), "Missing stone tool tag for " + oreId);
        }
    }

    @Test
    void machineProcessingChainHasBaselineRecipes() {
        Path generatedRecipes = RESOURCES.resolve("data/extremecraft/recipes/generated");
        assertTrue(Files.exists(generatedRecipes.resolve("machine_crusher.json")), "Missing machine recipe: crusher");
        assertTrue(Files.exists(generatedRecipes.resolve("machine_smelter.json")), "Missing machine recipe: smelter");
        assertTrue(Files.exists(generatedRecipes.resolve("machine_quantum_fabricator.json")), "Missing machine recipe: quantum_fabricator");
        assertTrue(Files.exists(generatedRecipes.resolve("special_quantum_processor.json")), "Missing special recipe: quantum_processor");
    }

    @Test
    void abilityHudAssetsExistForFallbackPath() {
        Path abilityDefault = RESOURCES.resolve("assets/extremecraft/textures/gui/abilities/ability_default.png");
        Path spellBookTexture = RESOURCES.resolve("assets/extremecraft/textures/item/spell_book.png");
        assertTrue(Files.exists(abilityDefault), "Missing fallback ability icon texture");
        assertTrue(Files.exists(spellBookTexture), "Missing spell book item texture");
    }

    @Test
    void researchUnlockSchemaUsesArrays() throws IOException {
        Path researchDir = RESOURCES.resolve("data/extremecraft/research");
        assertTrue(Files.exists(researchDir), "Missing research data directory");

        try (var files = Files.list(researchDir)) {
            files.filter(path -> path.toString().endsWith(".json")).forEach(path -> {
                try {
                    JsonObject root = readJson(path);
                    JsonElement unlocks = root.get("unlocks");
                    if (unlocks != null && !unlocks.isJsonNull()) {
                        assertTrue(unlocks.isJsonArray(), "Research unlocks must be an array in " + path.getFileName());
                    }
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    @Test
    void blockLootTablesExistForRegisteredBlocks() throws IOException {
        Set<String> blockIds = allBlockIds();
        Path lootDir = RESOURCES.resolve("data/extremecraft/loot_tables/blocks");

        for (String blockId : blockIds) {
            assertTrue(Files.exists(lootDir.resolve(blockId + ".json")), "Missing block loot table for " + blockId);
        }

        assertFalse(Files.exists(lootDir.resolve("singularity_ore_ore.json")), "Duplicated ore suffix loot table should not exist");
    }

    private static Set<String> oreBlockIds() throws IOException {
        Set<String> result = new LinkedHashSet<>();
        for (String materialId : materialIds()) {
            result.add(normalizeOreId(materialId));
        }
        return result;
    }

    private static Set<String> allBlockIds() throws IOException {
        Set<String> result = new LinkedHashSet<>();
        result.addAll(readInlineIds(ROOT.resolve("src/main/java/com/extremecraft/registry/ModBlocks.java"), "BLOCKS"));
        result.addAll(machineIds());
        result.addAll(cableIds());

        for (String materialId : materialIds()) {
            result.add(normalizeOreId(materialId));
            result.add(materialId + "_block");
        }
        return result;
    }

    private static Set<String> materialIds() throws IOException {
        Pattern pattern = Pattern.compile("new OreMaterialDefinition\\(\\\"([a-z0-9_]+)\\\"");
        return findPattern(ROOT.resolve("src/main/java/com/extremecraft/machine/material/OreMaterialCatalog.java"), pattern);
    }

    private static Set<String> machineIds() throws IOException {
        Pattern pattern = Pattern.compile("new MachineDefinition\\(\\\"([a-z0-9_]+)\\\"");
        return findPattern(ROOT.resolve("src/main/java/com/extremecraft/machine/core/MachineCatalog.java"), pattern);
    }

    private static Set<String> cableIds() throws IOException {
        Pattern pattern = Pattern.compile("\\(\\\"([a-z0-9_]+)\\\",\\s*\\d+\\)");
        return findPattern(ROOT.resolve("src/main/java/com/extremecraft/machine/cable/CableTier.java"), pattern);
    }

    private static Set<String> readInlineIds(Path path, String registerOwner) throws IOException {
        Pattern pattern = Pattern.compile(registerOwner + "\\.register\\(\\\"([a-z0-9_]+)\\\"");
        return findPattern(path, pattern);
    }

    private static Set<String> findPattern(Path path, Pattern pattern) throws IOException {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        Matcher matcher = pattern.matcher(source);
        Set<String> ids = new LinkedHashSet<>();
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private static JsonObject readJson(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static Set<String> jsonStringSet(JsonObject root, String key) {
        Set<String> values = new LinkedHashSet<>();
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return values;
        }
        for (JsonElement element : root.getAsJsonArray(key)) {
            if (element.isJsonPrimitive()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    private static String normalizeOreId(String materialId) {
        return materialId.endsWith("_ore") ? materialId : materialId + "_ore";
    }
}
