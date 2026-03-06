package com.extremecraft.machine;

import java.util.List;

public record MachineDefinition(
        String id,
        String tier,
        int inputSlots,
        int outputSlots,
        int processTicks,
        int energyPerTick,
        boolean supportsForgeEnergy,
        boolean supportsEcEnergy,
        List<String> recipeIds
) {
}
