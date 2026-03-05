package com.extremecraft.progression.skilltree;

import java.util.List;

/**
 * One skill node in a 2D graph used by the tree UI.
 */
public record SkillNode(
        String id,
        int x,
        int y,
        int cost,
        List<String> requiredNodes,
        List<SkillModifier> modifiers,
        String bonusText,
        String iconPath
) {
    public String resolvedIconPath() {
        if (iconPath != null && !iconPath.isBlank()) {
            return iconPath;
        }
        return "textures/gui/skills/" + id + ".png";
    }
}
