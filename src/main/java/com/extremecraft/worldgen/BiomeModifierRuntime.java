package com.extremecraft.worldgen;

import com.extremecraft.platform.data.definition.WorldGenerationDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BiomeModifierRuntime {
    private BiomeModifierRuntime() {
    }

    public static Map<String, Integer> buildBiomeWeightMap() {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (WorldGenerationDefinition definition : OreGenerationManager.oreProfiles()) {
            weights.put(definition.id(), definition.weight());
        }
        return Map.copyOf(weights);
    }
}
