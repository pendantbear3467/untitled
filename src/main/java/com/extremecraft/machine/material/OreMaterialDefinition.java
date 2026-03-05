package com.extremecraft.machine.material;

import java.util.Set;

public record OreMaterialDefinition(
        String id,
        MaterialTier tier,
        boolean hasTools,
        boolean hasArmor,
        int harvestLevel,
        int minY,
        int maxY,
        int veinsPerChunk,
        int veinSize,
        Set<String> dimensions
) {
}
