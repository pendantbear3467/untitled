package com.extremecraft.worldgen;

import com.extremecraft.platform.data.definition.StructureDefinition;
import com.extremecraft.platform.data.registry.StructureDataRegistry;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public final class StructureGenerationManager {
    private StructureGenerationManager() {
    }

    public static Collection<StructureDefinition> structures() {
        return StructureDataRegistry.registry().all();
    }

    public static List<StructureDefinition> weightedOrder() {
        return structures().stream()
                .sorted(Comparator.comparingInt(StructureDefinition::weight).reversed())
                .toList();
    }
}
