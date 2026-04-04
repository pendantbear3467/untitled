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
        int requiredLevel,
        List<String> requiredNodes,
        List<SkillModifier> modifiers,
        String displayName,
        String description,
        String iconPath
) {
    public SkillNode(String id, int x, int y, int cost, List<String> requiredNodes, List<SkillModifier> modifiers, String bonusText, String iconPath) {
        this(id, x, y, cost, 1, requiredNodes, modifiers, humanizeId(id), bonusText, iconPath);
    }

    public String bonusText() {
        return description;
    }

    public String resolvedDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        return humanizeId(id);
    }

    public String resolvedDescription() {
        if (description != null && !description.isBlank()) {
            return description;
        }
        return "No description";
    }

    public String resolvedIconPath() {
        if (iconPath != null && !iconPath.isBlank()) {
            return iconPath;
        }
        return "textures/gui/skills/" + id + ".png";
    }

    private static String humanizeId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return "Unknown Skill";
        }

        String[] parts = rawId.split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
