package com.extremecraft.progression.skilltree;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

public final class SkillTreeManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, List<SkillNode>> TREES = new LinkedHashMap<>();
    private static final Map<String, SkillNode> NODE_BY_ID = new LinkedHashMap<>();
    private static final Map<String, String> NODE_TREE_BY_ID = new LinkedHashMap<>();
    private static final Map<String, List<Connection>> CONNECTION_CACHE = new LinkedHashMap<>();
    private static final Set<String> TREE_IDS = new LinkedHashSet<>();

    static {
        loadDefaults();
    }

    public static synchronized void loadFromJson(Map<ResourceLocation, JsonElement> jsonMap) {
        Map<String, List<SkillNode>> loadedTrees = new LinkedHashMap<>();

        for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
            try {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject root = entry.getValue().getAsJsonObject();
                String treeId;
                if (root.has("tree")) {
                    treeId = root.get("tree").getAsString();
                } else if (root.has("tree_id")) {
                    treeId = root.get("tree_id").getAsString();
                } else {
                    treeId = entry.getKey().getPath();
                }

                if (treeId.contains("/")) {
                    treeId = treeId.substring(treeId.lastIndexOf('/') + 1);
                }
                treeId = treeId.trim().toLowerCase();
                if (treeId.isBlank()) {
                    LOGGER.warn("[SkillTree] Skipping tree with blank id from {}", entry.getKey());
                    continue;
                }

                JsonArray nodesJson = root.has("nodes") && root.get("nodes").isJsonArray() ? root.getAsJsonArray("nodes") : new JsonArray();
                List<SkillNode> nodes = new ArrayList<>();

                for (JsonElement nodeElement : nodesJson) {
                    try {
                        if (!nodeElement.isJsonObject()) {
                            continue;
                        }

                        JsonObject node = nodeElement.getAsJsonObject();
                        String id = node.has("id") ? node.get("id").getAsString().trim().toLowerCase() : "";
                        if (id.isBlank()) {
                            continue;
                        }

                        int x = node.has("x") ? node.get("x").getAsInt() : 0;
                        int y = node.has("y") ? node.get("y").getAsInt() : 0;
                        int cost = Math.max(1, node.has("cost") ? node.get("cost").getAsInt() : 1);
                        int requiredLevel = Math.max(1, node.has("requiredLevel") ? node.get("requiredLevel").getAsInt() : 1);

                        List<String> requires = new ArrayList<>();
                        if (node.has("requires") && node.get("requires").isJsonArray()) {
                            for (JsonElement req : node.getAsJsonArray("requires")) {
                                String reqId = req.getAsString().trim().toLowerCase();
                                if (!reqId.isBlank()) {
                                    requires.add(reqId);
                                }
                            }
                        }
                        if (node.has("requiredNodes") && node.get("requiredNodes").isJsonArray()) {
                            for (JsonElement req : node.getAsJsonArray("requiredNodes")) {
                                String reqId = req.getAsString().trim().toLowerCase();
                                if (!reqId.isBlank() && !requires.contains(reqId)) {
                                    requires.add(reqId);
                                }
                            }
                        }
                        if (node.has("required_nodes") && node.get("required_nodes").isJsonArray()) {
                            for (JsonElement req : node.getAsJsonArray("required_nodes")) {
                                String reqId = req.getAsString().trim().toLowerCase();
                                if (!reqId.isBlank() && !requires.contains(reqId)) {
                                    requires.add(reqId);
                                }
                            }
                        }

                        List<SkillModifier> modifiers = new ArrayList<>();
                        if (node.has("modifiers") && node.get("modifiers").isJsonArray()) {
                            for (JsonElement modElement : node.getAsJsonArray("modifiers")) {
                                if (!modElement.isJsonObject()) {
                                    continue;
                                }

                                JsonObject mod = modElement.getAsJsonObject();
                                String modifierId = mod.has("type") ? mod.get("type").getAsString().trim().toLowerCase() : "";
                                double value = mod.has("value") ? mod.get("value").getAsDouble() : 0.0D;
                                String operation = mod.has("operation") ? mod.get("operation").getAsString() : "ADD";

                                if (!modifierId.isBlank()) {
                                    modifiers.add(new SkillModifier(modifierId, value, SkillModifier.Operation.byName(operation)));
                                }
                            }
                        } else if (node.has("modifiers") && node.get("modifiers").isJsonObject()) {
                            JsonObject modsObject = node.getAsJsonObject("modifiers");
                            for (Map.Entry<String, JsonElement> modEntry : modsObject.entrySet()) {
                                String modifierId = modEntry.getKey() == null ? "" : modEntry.getKey().trim().toLowerCase();
                                if (modifierId.isBlank()) {
                                    continue;
                                }

                                JsonElement valueElement = modEntry.getValue();
                                if (valueElement == null || valueElement.isJsonNull()) {
                                    continue;
                                }

                                if (valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isNumber()) {
                                    modifiers.add(new SkillModifier(modifierId, valueElement.getAsDouble(), SkillModifier.Operation.ADD));
                                    continue;
                                }

                                if (valueElement.isJsonObject()) {
                                    JsonObject modObject = valueElement.getAsJsonObject();
                                    double value = modObject.has("value") ? modObject.get("value").getAsDouble() : 0.0D;
                                    String operation = modObject.has("operation") ? modObject.get("operation").getAsString() : "ADD";
                                    modifiers.add(new SkillModifier(modifierId, value, SkillModifier.Operation.byName(operation)));
                                }
                            }
                        }

                        // Legacy support: older trees used statModifiers object instead of modifiers.
                        if (modifiers.isEmpty() && node.has("statModifiers") && node.get("statModifiers").isJsonObject()) {
                            JsonObject legacyMods = node.getAsJsonObject("statModifiers");
                            for (Map.Entry<String, JsonElement> modEntry : legacyMods.entrySet()) {
                                String modifierId = modEntry.getKey() == null ? "" : modEntry.getKey().trim().toLowerCase();
                                JsonElement valueElement = modEntry.getValue();
                                if (modifierId.isBlank() || valueElement == null || valueElement.isJsonNull()) {
                                    continue;
                                }

                                if (valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isNumber()) {
                                    modifiers.add(new SkillModifier(modifierId, valueElement.getAsDouble(), SkillModifier.Operation.ADD));
                                }
                            }
                        }

                        String displayName = node.has("displayName") ? node.get("displayName").getAsString() : id.replace('_', ' ');
                        String description = node.has("description") ? node.get("description").getAsString()
                                : (node.has("bonus") ? node.get("bonus").getAsString() : "");
                        String icon = node.has("icon") ? node.get("icon").getAsString() : "";
                        nodes.add(new SkillNode(id, x, y, cost, requiredLevel, List.copyOf(requires), List.copyOf(modifiers), displayName, description, icon));
                    } catch (RuntimeException nodeEx) {
                        LOGGER.warn("[SkillTree] Skipping malformed node in {}: {}", entry.getKey(), nodeEx.getMessage());
                    }
                }

                if (!nodes.isEmpty()) {
                    loadedTrees.put(treeId, List.copyOf(nodes));
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("[SkillTree] Skipping malformed tree {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (loadedTrees.isEmpty()) {
            loadDefaults();
            return;
        }

        replaceAll(loadedTrees);
    }

    public static synchronized List<String> treeIds() {
        return List.copyOf(TREE_IDS);
    }

    public static synchronized List<SkillNode> nodesForTree(String treeId) {
        return TREES.getOrDefault(treeId, List.of());
    }

    public static synchronized List<Connection> connectionsForTree(String treeId) {
        return CONNECTION_CACHE.getOrDefault(treeId, List.of());
    }

    public static synchronized SkillNode getNode(String nodeId) {
        return NODE_BY_ID.get(nodeId);
    }

    public static synchronized String treeIdForNode(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return "";
        }
        String normalizedNodeId = nodeId.trim().toLowerCase(Locale.ROOT);
        return NODE_TREE_BY_ID.getOrDefault(normalizedNodeId, "");
    }

    public static synchronized Map<String, List<SkillNode>> allTrees() {
        return Collections.unmodifiableMap(TREES);
    }

    private static void loadDefaults() {
        Map<String, List<SkillNode>> defaults = new LinkedHashMap<>();

        defaults.put("combat", List.of(
                node("combat_damage_mastery", -100, -30, 1, List.of(), List.of(new SkillModifier("damage_bonus", 2.0D, SkillModifier.Operation.ADD)), "+2 melee damage"),
                node("combat_weapon_efficiency", -40, -10, 1, List.of("combat_damage_mastery"), List.of(new SkillModifier("stamina_cost_reduction", 0.06D, SkillModifier.Operation.ADD)), "Reduced stamina cost"),
                node("combat_critical_strikes", 20, 10, 2, List.of("combat_weapon_efficiency"), List.of(new SkillModifier("crit_chance_bonus", 0.05D, SkillModifier.Operation.ADD)), "+5% critical chance"),
                node("combat_berserker_rage", 80, 30, 3, List.of("combat_critical_strikes"), List.of(new SkillModifier("damage_multiplier", 1.10D, SkillModifier.Operation.MULTIPLY)), "10% damage multiplier")
        ));

        defaults.put("survival", List.of(
                node("survival_tough_skin", -100, -30, 1, List.of(), List.of(new SkillModifier("damage_multiplier", 0.97D, SkillModifier.Operation.MULTIPLY)), "Damage resistance"),
                node("survival_regeneration", -40, -10, 1, List.of("survival_tough_skin"), List.of(new SkillModifier("mana_regeneration", 0.5D, SkillModifier.Operation.ADD)), "Resource regeneration"),
                node("survival_iron_stomach", 20, 10, 2, List.of("survival_regeneration"), List.of(new SkillModifier("stamina_cost_reduction", 0.05D, SkillModifier.Operation.ADD)), "Reduced drain"),
                node("survival_environmental_resistance", 80, 30, 3, List.of("survival_iron_stomach"), List.of(new SkillModifier("damage_multiplier", 0.95D, SkillModifier.Operation.MULTIPLY)), "Environmental resistance")
        ));

        defaults.put("explorer", List.of(
                node("explorer_treasure_hunter", -100, -30, 1, List.of(), List.of(new SkillModifier("loot_rarity_bonus", 0.04D, SkillModifier.Operation.ADD)), "Rare loot bonus"),
                node("explorer_rare_loot_finder", -40, -10, 1, List.of("explorer_treasure_hunter"), List.of(new SkillModifier("loot_rarity_bonus", 0.06D, SkillModifier.Operation.ADD)), "Better chest loot"),
                node("explorer_mob_tracking", 20, 10, 2, List.of("explorer_rare_loot_finder"), List.of(), "Tracking utility"),
                node("explorer_night_vision", 80, 30, 2, List.of("explorer_mob_tracking"), List.of(), "Vision utility")
        ));

        defaults.put("arcane", List.of(
                node("arcane_mana_flow", -100, -30, 1, List.of(), List.of(new SkillModifier("mana_regeneration", 1.0D, SkillModifier.Operation.ADD)), "Passive mana regeneration"),
                node("arcane_spell_amplification", -40, -10, 1, List.of("arcane_mana_flow"), List.of(new SkillModifier("spell_power_bonus", 0.10D, SkillModifier.Operation.PERCENT)), "Spell amplification"),
                node("arcane_mana_regeneration", 20, 10, 2, List.of("arcane_spell_amplification"), List.of(new SkillModifier("mana_regeneration", 1.5D, SkillModifier.Operation.ADD)), "Faster mana regen"),
                node("arcane_elemental_affinity", 80, 30, 3, List.of("arcane_mana_regeneration"), List.of(new SkillModifier("spell_power_bonus", 0.15D, SkillModifier.Operation.PERCENT)), "Elemental affinity")
        ));

        replaceAll(defaults);
    }

    private static SkillNode node(String id, int x, int y, int cost, List<String> requiredNodes, List<SkillModifier> modifiers, String bonusText) {
        return new SkillNode(id, x, y, cost, 1, List.copyOf(requiredNodes), List.copyOf(modifiers), id.replace('_', ' '), bonusText, "");
    }

    private static void replaceAll(Map<String, List<SkillNode>> trees) {
        TREES.clear();
        NODE_BY_ID.clear();
        NODE_TREE_BY_ID.clear();
        CONNECTION_CACHE.clear();
        TREE_IDS.clear();

        for (Map.Entry<String, List<SkillNode>> entry : trees.entrySet()) {
            String treeId = entry.getKey();
            if (treeId == null || treeId.isBlank()) {
                continue;
            }

            Map<String, SkillNode> uniqueNodes = new LinkedHashMap<>();
            for (SkillNode node : entry.getValue()) {
                if (node == null || node.id() == null || node.id().isBlank()) {
                    continue;
                }

                String nodeId = node.id();
                if (uniqueNodes.containsKey(nodeId)) {
                    LOGGER.warn("[SkillTree] Duplicate node id '{}' in tree '{}' - keeping first definition", nodeId, treeId);
                    continue;
                }

                String ownerTree = NODE_TREE_BY_ID.get(nodeId);
                if (ownerTree != null && !ownerTree.equals(treeId)) {
                    LOGGER.warn("[SkillTree] Node id '{}' is defined in both '{}' and '{}' - skipping duplicate in '{}'",
                            nodeId, ownerTree, treeId, treeId);
                    continue;
                }

                uniqueNodes.put(nodeId, node);
                NODE_TREE_BY_ID.put(nodeId, treeId);
                NODE_BY_ID.put(nodeId, node);
            }

            if (!uniqueNodes.isEmpty()) {
                List<SkillNode> nodes = List.copyOf(uniqueNodes.values());
                TREES.put(treeId, nodes);
                TREE_IDS.add(treeId);
            }
        }

        buildConnectionCache();
    }

    private static void buildConnectionCache() {
        TREES.forEach((treeId, nodes) -> {
            List<Connection> connections = new ArrayList<>();
            Map<String, SkillNode> localNodes = new LinkedHashMap<>();
            for (SkillNode node : nodes) {
                localNodes.put(node.id(), node);
            }

            for (SkillNode node : nodes) {
                for (String required : node.requiredNodes()) {
                    SkillNode source = localNodes.get(required);
                    if (source != null) {
                        connections.add(new Connection(source.id(), node.id()));
                        continue;
                    }

                    LOGGER.warn("[SkillTree] Node '{}' in tree '{}' references missing requirement '{}'",
                            node.id(), treeId, required);
                }
            }

            CONNECTION_CACHE.put(treeId, List.copyOf(connections));
        });
    }

    public record Connection(String from, String to) {
    }

    private SkillTreeManager() {
    }
}

