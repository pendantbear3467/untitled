package com.extremecraft.item.module;

import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runtime registry for loaded modules.
 */
public final class ModuleRegistry {
    private static final Map<String, ModuleDefinition> DEFINITIONS = new LinkedHashMap<>();

    private ModuleRegistry() {
    }

    public static synchronized void replaceAll(Map<String, ModuleDefinition> modules) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(modules);
    }

    public static synchronized ModuleDefinition get(String id) {
        if (id == null) {
            return null;
        }
        return DEFINITIONS.get(id.trim().toLowerCase());
    }

    public static synchronized List<ModuleDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static float sumToolEffect(ItemStack stack, String effectId) {
        if (stack == null || stack.isEmpty() || effectId == null || effectId.isBlank()) {
            return 0.0F;
        }

        String key = effectId.trim().toLowerCase();
        float total = 0.0F;
        for (ItemModuleStorage.InstalledModule installed : ItemModuleStorage.getModules(stack)) {
            ModuleDefinition def = get(installed.id());
            if (def == null || !def.supportsTool()) {
                continue;
            }
            total += def.effects().getOrDefault(key, 0.0F) * Math.max(1, installed.level());
        }
        return total;
    }
}
