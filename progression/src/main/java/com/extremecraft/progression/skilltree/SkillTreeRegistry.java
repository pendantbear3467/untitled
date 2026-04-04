package com.extremecraft.progression.skilltree;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compatibility mirror for older client screens that still expect a materialized skill-tree
 * registry.
 *
 * <p>Canonical live skill-tree topology loads into {@code SkillTreeManager}; new mutation or gate
 * logic should not be added here.</p>
 */
public final class SkillTreeRegistry {
    private static final Map<String, SkillTree> TREES = new LinkedHashMap<>();

    private SkillTreeRegistry() {}

    public static void replaceAll(Map<String, SkillTree> trees) {
        TREES.clear();
        TREES.putAll(trees);
    }

    public static SkillTree get(String id) {
        return TREES.get(id);
    }

    public static Collection<SkillTree> all() {
        return TREES.values();
    }

    public static SkillTree first() {
        return TREES.values().stream().findFirst().orElse(null);
    }
}
