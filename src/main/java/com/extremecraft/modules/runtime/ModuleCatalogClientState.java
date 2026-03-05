package com.extremecraft.modules.runtime;

import java.util.ArrayList;
import java.util.List;

public final class ModuleCatalogClientState {
    public record ModuleEntry(String id, int slotCost, String requiredSkillNode) {}

    private static List<ModuleEntry> armorModules = List.of();
    private static List<ModuleEntry> toolModules = List.of();

    private ModuleCatalogClientState() {
    }

    public static synchronized void apply(List<ModuleEntry> armor, List<ModuleEntry> tools) {
        armorModules = List.copyOf(armor);
        toolModules = List.copyOf(tools);
    }

    public static synchronized List<ModuleEntry> armorModules() {
        return new ArrayList<>(armorModules);
    }

    public static synchronized List<ModuleEntry> toolModules() {
        return new ArrayList<>(toolModules);
    }
}
