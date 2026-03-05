package com.extremecraft.progression.tech;

import java.util.LinkedHashSet;
import java.util.Set;

public record UnlockRequirement(
        MachineTier tier,
        Set<String> requiredMachines,
        Set<String> requiredMaterials
) {
    public UnlockRequirement {
        requiredMachines = sanitize(requiredMachines);
        requiredMaterials = sanitize(requiredMaterials);
    }

    public static UnlockRequirement of(MachineTier tier, Set<String> requiredMachines, Set<String> requiredMaterials) {
        return new UnlockRequirement(tier, requiredMachines, requiredMaterials);
    }

    public boolean isSatisfied(Set<String> unlockedMachines, Set<String> unlockedMaterials) {
        return unlockedMachines.containsAll(requiredMachines) && unlockedMaterials.containsAll(requiredMaterials);
    }

    public Set<String> missingMachines(Set<String> unlockedMachines) {
        Set<String> missing = new LinkedHashSet<>(requiredMachines);
        missing.removeAll(unlockedMachines);
        return missing;
    }

    public Set<String> missingMaterials(Set<String> unlockedMaterials) {
        Set<String> missing = new LinkedHashSet<>(requiredMaterials);
        missing.removeAll(unlockedMaterials);
        return missing;
    }

    private static Set<String> sanitize(Set<String> values) {
        Set<String> clean = new LinkedHashSet<>();
        if (values == null) {
            return clean;
        }

        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                clean.add(trimmed);
            }
        }
        return clean;
    }
}
