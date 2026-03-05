package com.extremecraft.modules.registry;

import com.extremecraft.modules.data.ModuleDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ToolModuleRegistry {
    private static final Map<String, ModuleDefinition> MODULES = new LinkedHashMap<>();

    private ToolModuleRegistry() {
    }

    public static synchronized void replaceAll(Map<String, ModuleDefinition> modules) {
        MODULES.clear();
        MODULES.putAll(modules);
    }

    public static synchronized ModuleDefinition get(String id) {
        return MODULES.get(id);
    }

    public static synchronized Collection<ModuleDefinition> all() {
        return java.util.List.copyOf(MODULES.values());
    }
}
