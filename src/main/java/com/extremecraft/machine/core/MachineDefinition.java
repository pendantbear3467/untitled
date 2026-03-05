package com.extremecraft.machine.core;

import com.extremecraft.progression.stage.ProgressionStage;

public record MachineDefinition(
        String id,
        MachineCategory category,
        ProgressionStage stage,
        int processTime,
        int energyPerTick,
        int outputMultiplier,
        int generationPerTick
) {
}
