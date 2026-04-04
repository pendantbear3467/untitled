package com.extremecraft.progression;

import java.util.Map;

public record ProgressionDefinition(String id, int baseXp, double levelMultiplier, Map<String, Double> statGrowth) {
}
