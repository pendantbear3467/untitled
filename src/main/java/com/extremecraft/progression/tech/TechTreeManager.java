package com.extremecraft.progression.tech;

import com.extremecraft.materials.ModMaterials;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class TechTreeManager {
    private static final Map<String, UnlockRequirement> MACHINE_REQUIREMENTS = new LinkedHashMap<>();

    static {
        // Tier 1: entry power + early ore processing.
        register("coal_generator", MachineTier.TIER_1, Set.of(), Set.of("copper", "iron"));
        register("crusher", MachineTier.TIER_1, Set.of("coal_generator"), Set.of("copper", "iron"));
        register("pulverizer", MachineTier.TIER_1, Set.of("coal_generator"), Set.of("copper", "tin"));
        register("basic_smelter", MachineTier.TIER_1, Set.of("coal_generator"), Set.of("copper", "iron"));

        // Tier 2: alloy pipeline and energy infrastructure.
        register("alloy_furnace", MachineTier.TIER_2, Set.of("crusher"), Set.of("steel"));
        register("compressor", MachineTier.TIER_2, Set.of("alloy_furnace"), Set.of("bronze", "steel"));
        register("advanced_generator", MachineTier.TIER_2, Set.of("compressor"), Set.of("steel", "titanium_alloy"));
        register("energy_storage_block", MachineTier.TIER_2, Set.of("alloy_furnace"), Set.of("steel", "bronze"));

        // Tier 3: multi-stage processing and controller automation.
        register("chemical_processor", MachineTier.TIER_3, Set.of("advanced_generator", "compressor"), Set.of("titanium", "titanium_alloy"));
        register("industrial_grinder", MachineTier.TIER_3, Set.of("chemical_processor"), Set.of("nickel", "titanium_alloy"));
        register("matter_infuser", MachineTier.TIER_3, Set.of("industrial_grinder"), Set.of("mythril", "mythril_alloy", "aetherium"));
        register("energy_network_controller", MachineTier.TIER_3, Set.of("advanced_generator", "energy_storage_block"), Set.of("titanium_alloy", "silver"));

        // Tier 4: endgame loop.
        register("fusion_reactor", MachineTier.TIER_4, Set.of("matter_infuser", "energy_network_controller"), Set.of("mythril_alloy", "void_crystal"));
        register("quantum_miner", MachineTier.TIER_4, Set.of("fusion_reactor"), Set.of("void_crystal", "titanium_alloy"));
        register("matter_assembler", MachineTier.TIER_4, Set.of("quantum_miner"), Set.of("aetherium", "mythril_alloy"));
        register("void_fabricator", MachineTier.TIER_4, Set.of("matter_assembler", "fusion_reactor"), Set.of("void_crystal", "aetherium"));
    }

    private static void register(String machineId, MachineTier tier, Set<String> machines, Set<String> materials) {
        MACHINE_REQUIREMENTS.put(machineId, UnlockRequirement.of(tier, machines, validateMaterials(materials)));
    }

    private static Set<String> validateMaterials(Set<String> materials) {
        Set<String> resolved = new LinkedHashSet<>();
        for (String material : materials) {
            String key = material.toLowerCase();
            if (ModMaterials.isKnownMaterial(key)) {
                resolved.add(key);
            }
        }
        return resolved;
    }

    public static Optional<UnlockRequirement> requirementForMachine(String machineId) {
        return Optional.ofNullable(MACHINE_REQUIREMENTS.get(normalize(machineId)));
    }

    public static boolean canUnlock(String machineId, Set<String> unlockedMachines, Set<String> unlockedMaterials) {
        UnlockRequirement requirement = MACHINE_REQUIREMENTS.get(normalize(machineId));
        if (requirement == null) {
            return true;
        }
        return requirement.isSatisfied(normalizeSet(unlockedMachines), normalizeSet(unlockedMaterials));
    }

    public static List<String> nextUnlockableMachines(Set<String> unlockedMachines, Set<String> unlockedMaterials) {
        Set<String> machineState = normalizeSet(unlockedMachines);
        Set<String> materialState = normalizeSet(unlockedMaterials);

        return MACHINE_REQUIREMENTS.entrySet().stream()
                .filter(entry -> !machineState.contains(entry.getKey()))
                .filter(entry -> entry.getValue().isSatisfied(machineState, materialState))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    public static boolean respectsCriticalUnlockChain(Set<String> unlockedMachines) {
        Set<String> machineState = normalizeSet(unlockedMachines);
        return hasChain(machineState, "coal_generator", "crusher")
                && hasChain(machineState, "crusher", "alloy_furnace")
                && hasChain(machineState, "alloy_furnace", "compressor");
    }

    public static Map<String, UnlockRequirement> machineRequirements() {
        return Collections.unmodifiableMap(MACHINE_REQUIREMENTS);
    }

    public static Set<String> machineIdsForTier(MachineTier tier) {
        return MACHINE_REQUIREMENTS.entrySet().stream()
                .filter(entry -> entry.getValue().tier() == tier)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static boolean hasChain(Set<String> machineState, String prerequisite, String unlocked) {
        if (!machineState.contains(unlocked)) {
            return true;
        }
        return machineState.contains(prerequisite);
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase();
    }

    private static Set<String> normalizeSet(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }

        for (String value : values) {
            String normalizedValue = normalize(value);
            if (!normalizedValue.isEmpty()) {
                normalized.add(normalizedValue);
            }
        }
        return normalized;
    }

    private TechTreeManager() {
    }
}
