package com.extremecraft.worldgen;

import com.extremecraft.machine.material.OreMaterialCatalog;
import com.extremecraft.machine.material.OreMaterialDefinition;

import java.util.Collection;

public final class OreGenerationProfiles {
    public static Collection<OreMaterialDefinition> all() {
        return OreMaterialCatalog.MATERIALS.values();
    }

    private OreGenerationProfiles() {
    }
}
