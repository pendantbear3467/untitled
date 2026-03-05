package com.extremecraft.progression.skilltree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkillTreeManager {
    private static final Map<String, List<SkillNode>> TREES = new LinkedHashMap<>();
    private static final Map<String, SkillNode> NODE_BY_ID = new LinkedHashMap<>();
    private static final Map<String, List<Connection>> CONNECTION_CACHE = new LinkedHashMap<>();

    static {
        registerTree("combat", List.of(
                node("combat_damage_mastery", -100, -30, 1, List.of(), "+2 melee damage"),
                node("combat_weapon_efficiency", -40, -10, 1, List.of("combat_damage_mastery"), "Reduced stamina cost for weapon attacks"),
                node("combat_critical_strikes", 20, 10, 2, List.of("combat_weapon_efficiency"), "+5% critical hit chance"),
                node("combat_berserker_rage", 80, 30, 3, List.of("combat_critical_strikes"), "Damage bonus below 30% health")
        ));

        registerTree("survival", List.of(
                node("survival_tough_skin", -100, -30, 1, List.of(), "Damage resistance against mobs"),
                node("survival_regeneration", -40, -10, 1, List.of("survival_tough_skin"), "Passive health regeneration"),
                node("survival_iron_stomach", 20, 10, 2, List.of("survival_regeneration"), "Reduced hunger drain"),
                node("survival_environmental_resistance", 80, 30, 3, List.of("survival_iron_stomach"), "Resistance to environmental damage")
        ));

        registerTree("explorer", List.of(
                node("explorer_treasure_hunter", -100, -30, 1, List.of(), "Higher rare loot chance"),
                node("explorer_rare_loot_finder", -40, -10, 1, List.of("explorer_treasure_hunter"), "Better loot quality from chests"),
                node("explorer_mob_tracking", 20, 10, 2, List.of("explorer_rare_loot_finder"), "Highlights nearby hostile mobs"),
                node("explorer_night_vision", 80, 30, 2, List.of("explorer_mob_tracking"), "Improved night visibility")
        ));

        registerTree("arcane", List.of(
                node("arcane_mana_flow", -100, -30, 1, List.of(), "Passive mana regeneration"),
                node("arcane_spell_amplification", -40, -10, 1, List.of("arcane_mana_flow"), "Increased spell damage"),
                node("arcane_mana_regeneration", 20, 10, 2, List.of("arcane_spell_amplification"), "Faster mana recovery"),
                node("arcane_elemental_affinity", 80, 30, 3, List.of("arcane_mana_regeneration"), "Elemental spell efficiency")
        ));

        buildConnectionCache();
    }

    private static void registerTree(String treeId, List<SkillNode> nodes) {
        TREES.put(treeId, List.copyOf(nodes));
        for (SkillNode node : nodes) {
            NODE_BY_ID.put(node.id(), node);
        }
    }

    private static SkillNode node(String id, int x, int y, int cost, List<String> requiredNodes, String bonusText) {
        return new SkillNode(id, x, y, cost, requiredNodes, Map.of(), bonusText);
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
                    }
                }
            }

            CONNECTION_CACHE.put(treeId, List.copyOf(connections));
        });
    }

    public static List<String> treeIds() {
        return List.copyOf(TREES.keySet());
    }

    public static List<SkillNode> nodesForTree(String treeId) {
        return TREES.getOrDefault(treeId, List.of());
    }

    public static List<Connection> connectionsForTree(String treeId) {
        return CONNECTION_CACHE.getOrDefault(treeId, List.of());
    }

    public static SkillNode getNode(String nodeId) {
        return NODE_BY_ID.get(nodeId);
    }

    public static Map<String, List<SkillNode>> allTrees() {
        return Collections.unmodifiableMap(TREES);
    }

    public record Connection(String from, String to) {
    }

    private SkillTreeManager() {
    }
}
