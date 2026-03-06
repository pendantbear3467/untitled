package com.extremecraft.worldgen;

import com.extremecraft.api.ExtremeCraftAPI;
import com.extremecraft.api.definition.WorldgenFeatureDefinition;
import com.extremecraft.platform.data.definition.WorldGenerationDefinition;
import com.extremecraft.platform.data.registry.WorldGenerationDataRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WorldgenFeatureRegistry {
    private WorldgenFeatureRegistry() {
    }

    public static Map<String, String> featureTypes() {
        Map<String, String> features = new LinkedHashMap<>();

        for (WorldGenerationDefinition definition : WorldGenerationDataRegistry.registry().all()) {
            features.put(definition.id(), definition.profileType());
        }

        for (WorldgenFeatureDefinition apiDefinition : ExtremeCraftAPI.worldgenFeatures()) {
            features.putIfAbsent(apiDefinition.id(), apiDefinition.featureType());
        }

        return Map.copyOf(features);
    }
}
