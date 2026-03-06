package com.extremecraft.platform.module;

import com.extremecraft.api.module.ExtremeCraftModule;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ModuleRegistry {
    private static final Map<String, ExtremeCraftModule> MODULES = new LinkedHashMap<>();

    private ModuleRegistry() {
    }

    public static synchronized void register(ExtremeCraftModule module) {
        MODULES.put(module.moduleId(), module);
    }

    public static synchronized Collection<ExtremeCraftModule> all() {
        return java.util.List.copyOf(MODULES.values());
    }

    public static synchronized Optional<ExtremeCraftModule> byId(String moduleId) {
        return Optional.ofNullable(MODULES.get(moduleId));
    }

    public static synchronized int size() {
        return MODULES.size();
    }
}
