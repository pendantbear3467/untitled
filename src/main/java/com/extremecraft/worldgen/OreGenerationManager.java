package com.extremecraft.worldgen;

import com.extremecraft.platform.data.definition.WorldGenerationDefinition;
import com.extremecraft.platform.data.registry.WorldGenerationDataRegistry;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class OreGenerationManager {
    private OreGenerationManager() {
    }

    public static Collection<WorldGenerationDefinition> oreProfiles() {
        return WorldGenerationDataRegistry.registry().all().stream()
                .filter(definition -> "ore".equalsIgnoreCase(definition.profileType()))
                .collect(Collectors.toUnmodifiableList());
    }

    public static List<String> oreProfileIds() {
        return oreProfiles().stream().map(WorldGenerationDefinition::id).toList();
    }
}
