package com.extremecraft.machines;

import java.util.List;

public final class MachineBlueprints {
    public static final List<String> ORE_PROCESSING_LINE = List.of(
            "Pulverizer T1 - ore to dust, ingot to powder",
            "Pulverizer T2 - doubled throughput",
            "Pulverizer T3 - byproduct extraction",
            "Smelter Forge T1 - dust/powder to ingot",
            "Smelter Forge T2 - alloy support"
    );

    public static final List<String> FARM_AUTOMATION_LINE = List.of(
            "Auto Farmer T1 - harvest and replant",
            "Auto Farmer T2 - fertilize and hydrate",
            "Auto Farmer T3 - crop routing and buffer"
    );

    public static final List<String> WINDMILL_LINE = List.of(
            "Windmill Rotor T1", "Windmill Rotor T2", "Windmill Rotor T3", "Windmill Rotor T4"
    );

    public static final List<String> GENERATOR_TIERS = List.of(
            "Generator Tier 1", "Generator Tier 2", "Generator Tier 3", "Generator Tier 4",
            "Generator Tier 5", "Generator Tier 6", "Generator Tier 7", "Generator Tier 8",
            "Generator Tier 9", "Generator Tier 10", "Generator Tier 11", "Generator Tier 12"
    );

    public static final List<String> REACTOR_LINE = List.of(
            "Fission Core T1", "Fission Core T2", "Containment T1", "Containment T2", "Coolant Injector",
            "Fuel Fabricator (liquid + ore)", "Waste Recycler", "Control Console"
    );

    private MachineBlueprints() {}
}
