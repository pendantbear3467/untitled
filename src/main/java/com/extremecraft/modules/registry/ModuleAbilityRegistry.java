package com.extremecraft.modules.registry;

import com.extremecraft.modules.data.ModuleAbilityDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleAbilityRegistry {
    private static final Map<String, ModuleAbilityDefinition> ABILITIES = new LinkedHashMap<>();

    private ModuleAbilityRegistry() {
    }

    public static synchronized void replaceAll(Map<String, ModuleAbilityDefinition> abilities) {
        ABILITIES.clear();
        ABILITIES.putAll(abilities);
    }

    public static synchronized int mergeMissing(Map<String, ModuleAbilityDefinition> abilities) {
        int merged = 0;
        for (Map.Entry<String, ModuleAbilityDefinition> entry : abilities.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            if (ABILITIES.putIfAbsent(entry.getKey(), entry.getValue()) == null) {
                merged++;
            }
        }
        return merged;
    }

    public static synchronized ModuleAbilityDefinition get(String id) {
        return ABILITIES.get(id);
    }

    public static synchronized Collection<ModuleAbilityDefinition> all() {
        return java.util.List.copyOf(ABILITIES.values());
    }

    public static synchronized int size() {
        return ABILITIES.size();
    }
}
