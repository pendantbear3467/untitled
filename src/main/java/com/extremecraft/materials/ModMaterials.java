package com.extremecraft.materials;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ModMaterials {
    private static final Map<String, MaterialTier> ORES = new LinkedHashMap<>();
    private static final Map<String, MaterialTier> ADVANCED_ORES = new LinkedHashMap<>();
    private static final Map<String, AlloyDefinition> ALLOYS = new LinkedHashMap<>();

    static {
        // Base ores
        ORES.put("copper", MaterialTier.TIER_1);
        ORES.put("tin", MaterialTier.TIER_1);
        ORES.put("silver", MaterialTier.TIER_2);
        ORES.put("lead", MaterialTier.TIER_2);
        ORES.put("nickel", MaterialTier.TIER_2);
        ORES.put("titanium", MaterialTier.TIER_3);

        // Advanced ores
        ADVANCED_ORES.put("mythril", MaterialTier.TIER_3);
        ADVANCED_ORES.put("aetherium", MaterialTier.TIER_4);
        ADVANCED_ORES.put("void_crystal", MaterialTier.TIER_4);

        // Alloy dependencies
        ALLOYS.put("bronze", new AlloyDefinition("bronze", MaterialTier.TIER_2, Set.of("copper", "tin")));
        ALLOYS.put("steel", new AlloyDefinition("steel", MaterialTier.TIER_2, Set.of("iron", "coal")));
        ALLOYS.put("titanium_alloy", new AlloyDefinition("titanium_alloy", MaterialTier.TIER_3, Set.of("titanium", "nickel")));
        ALLOYS.put("mythril_alloy", new AlloyDefinition("mythril_alloy", MaterialTier.TIER_4, Set.of("mythril", "aetherium")));
    }

    public static boolean isKnownMaterial(String id) {
        String key = normalize(id);
        return ORES.containsKey(key) || ADVANCED_ORES.containsKey(key) || ALLOYS.containsKey(key)
                || "iron".equals(key) || "coal".equals(key);
    }

    public static Optional<MaterialTier> tierOf(String id) {
        String key = normalize(id);
        MaterialTier oreTier = ORES.get(key);
        if (oreTier != null) {
            return Optional.of(oreTier);
        }

        MaterialTier advancedTier = ADVANCED_ORES.get(key);
        if (advancedTier != null) {
            return Optional.of(advancedTier);
        }

        AlloyDefinition alloy = ALLOYS.get(key);
        if (alloy != null) {
            return Optional.of(alloy.tier());
        }

        return Optional.empty();
    }

    public static Map<String, MaterialTier> ores() {
        return Collections.unmodifiableMap(ORES);
    }

    public static Map<String, MaterialTier> advancedOres() {
        return Collections.unmodifiableMap(ADVANCED_ORES);
    }

    public static Map<String, AlloyDefinition> alloys() {
        return Collections.unmodifiableMap(ALLOYS);
    }

    public static Set<String> requirementsForAlloy(String alloyId) {
        AlloyDefinition definition = ALLOYS.get(normalize(alloyId));
        if (definition == null) {
            return Set.of();
        }
        return definition.inputs();
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    private ModMaterials() {
    }

    public record AlloyDefinition(String id, MaterialTier tier, Set<String> inputs) {
        public AlloyDefinition {
            Set<String> normalizedInputs = new LinkedHashSet<>();
            if (inputs != null) {
                for (String input : inputs) {
                    if (input != null && !input.isBlank()) {
                        normalizedInputs.add(input.trim().toLowerCase());
                    }
                }
            }
            inputs = Collections.unmodifiableSet(normalizedInputs);
            id = id == null ? "" : id.trim().toLowerCase();
        }
    }
}
