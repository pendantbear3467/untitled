package com.extremecraft.worldgen;

import com.extremecraft.materials.MaterialTier;
import com.extremecraft.materials.ModMaterials;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Central worldgen progression metadata for future PlacedFeature bindings.
 */
public final class ModWorldGen {
    private static final Map<MaterialTier, Set<String>> ORES_BY_TIER = new LinkedHashMap<>();

    static {
        ORES_BY_TIER.put(MaterialTier.TIER_1, Set.of("copper", "tin"));
        ORES_BY_TIER.put(MaterialTier.TIER_2, Set.of("silver", "lead", "nickel"));
        ORES_BY_TIER.put(MaterialTier.TIER_3, Set.of("titanium", "mythril"));
        ORES_BY_TIER.put(MaterialTier.TIER_4, Set.of("aetherium", "void_crystal"));
    }

    public static Map<MaterialTier, Set<String>> oresByTier() {
        return Map.copyOf(ORES_BY_TIER);
    }

    public static boolean supportsWorldgen(String materialId) {
        String id = materialId == null ? "" : materialId.trim().toLowerCase();
        return ModMaterials.ores().containsKey(id) || ModMaterials.advancedOres().containsKey(id);
    }

    private ModWorldGen() {
    }
}
