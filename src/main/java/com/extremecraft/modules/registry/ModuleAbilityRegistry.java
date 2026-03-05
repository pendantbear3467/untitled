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

    public static synchronized ModuleAbilityDefinition get(String id) {
        return ABILITIES.get(id);
    }

    public static synchronized Collection<ModuleAbilityDefinition> all() {
        return java.util.List.copyOf(ABILITIES.values());
    }
}
