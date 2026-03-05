package com.extremecraft.research;

import com.extremecraft.progression.stage.ProgressionStage;

import java.util.List;

public record ResearchDefinition(
        String id,
        ProgressionStage requiredStage,
        List<String> unlocks
) {
}
