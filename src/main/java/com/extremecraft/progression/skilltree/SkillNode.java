package com.extremecraft.progression.skilltree;

import java.util.List;
import java.util.Map;

/**
 * One skill node in a 2D graph used by the tree UI.
 */
public record SkillNode(
        String id,
        int x,
        int y,
        int cost,
        List<String> requiredNodes,
        Map<String, Double> statModifiers,
        String bonusText
) {
}
