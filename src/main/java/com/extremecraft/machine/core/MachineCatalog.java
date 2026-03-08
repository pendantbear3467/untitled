package com.extremecraft.machine.core;

import com.extremecraft.progression.stage.ProgressionStage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class MachineCatalog {
    public static final Map<String, MachineDefinition> DEFINITIONS = new LinkedHashMap<>();

    static {
        register(new MachineDefinition("alloy_furnace", MachineCategory.PROCESSOR, ProgressionStage.INDUSTRIAL, 160, 24, 1, 0));
        register(new MachineDefinition("crusher", MachineCategory.PROCESSOR, ProgressionStage.PRIMITIVE, 200, 16, 2, 0));
        register(new MachineDefinition("smelter", MachineCategory.PROCESSOR, ProgressionStage.PRIMITIVE, 240, 18, 1, 0));
        register(new MachineDefinition("compressor", MachineCategory.PROCESSOR, ProgressionStage.INDUSTRIAL, 200, 35, 1, 0));

        register(new MachineDefinition("coal_generator", MachineCategory.GENERATOR, ProgressionStage.PRIMITIVE, 0, 0, 0, 40));
        register(new MachineDefinition("steam_generator", MachineCategory.GENERATOR, ProgressionStage.ENERGY, 0, 0, 0, 70));
        register(new MachineDefinition("solar_generator", MachineCategory.GENERATOR, ProgressionStage.ENERGY, 0, 0, 0, 25));
        register(new MachineDefinition("fusion_reactor", MachineCategory.GENERATOR, ProgressionStage.ADVANCED, 0, 0, 0, 450));

        register(new MachineDefinition("electric_furnace", MachineCategory.PROCESSOR, ProgressionStage.ENERGY, 120, 36, 1, 0));
        register(new MachineDefinition("enrichment_chamber", MachineCategory.PROCESSOR, ProgressionStage.ENERGY, 160, 48, 2, 0));
        register(new MachineDefinition("advanced_pulverizer", MachineCategory.PROCESSOR, ProgressionStage.ENERGY, 100, 55, 2, 0));
        register(new MachineDefinition("fluid_extractor", MachineCategory.PROCESSOR, ProgressionStage.ENERGY, 180, 50, 1, 0));
        register(new MachineDefinition("industrial_generator", MachineCategory.GENERATOR, ProgressionStage.ENERGY, 0, 0, 0, 120));

        register(new MachineDefinition("fusion_alloy_smelter", MachineCategory.PROCESSOR, ProgressionStage.ADVANCED, 180, 120, 1, 0));
        register(new MachineDefinition("quantum_fabricator", MachineCategory.PROCESSOR, ProgressionStage.ADVANCED, 260, 180, 1, 0));
        register(new MachineDefinition("matter_converter", MachineCategory.PROCESSOR, ProgressionStage.ADVANCED, 240, 200, 1, 0));
        register(new MachineDefinition("void_reactor", MachineCategory.GENERATOR, ProgressionStage.ADVANCED, 0, 0, 0, 700));

        register(new MachineDefinition("rune_infuser", MachineCategory.MAGIC, ProgressionStage.ADVANCED, 200, 140, 1, 0));
        register(new MachineDefinition("mana_extractor", MachineCategory.MAGIC, ProgressionStage.ADVANCED, 180, 120, 1, 0));
        register(new MachineDefinition("arcane_forge", MachineCategory.MAGIC, ProgressionStage.ADVANCED, 220, 160, 1, 0));

        register(new MachineDefinition("singularity_compressor", MachineCategory.ENDGAME, ProgressionStage.ENDGAME, 360, 480, 1, 0));
        register(new MachineDefinition("planetary_extractor", MachineCategory.ENDGAME, ProgressionStage.ENDGAME, 300, 420, 2, 0));
        register(new MachineDefinition("dimensional_reactor", MachineCategory.ENDGAME, ProgressionStage.ENDGAME, 0, 0, 0, 1200));
    }

    private static void register(MachineDefinition definition) {
        DEFINITIONS.put(definition.id(), definition);
    }

    public static Optional<MachineDefinition> byId(String id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    private MachineCatalog() {
    }
}

