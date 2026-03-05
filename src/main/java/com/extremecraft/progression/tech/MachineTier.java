package com.extremecraft.progression.tech;

public enum MachineTier {
    TIER_1("Tier 1", 1, "Bootstrap power generation and basic ore processing"),
    TIER_2("Tier 2", 2, "Alloys, throughput upgrades, and energy infrastructure"),
    TIER_3("Tier 3", 3, "Multi-stage processing and scaled automation"),
    TIER_4("Tier 4", 4, "Endgame power and matter manipulation");

    private final String label;
    private final int order;
    private final String purpose;

    MachineTier(String label, int order, String purpose) {
        this.label = label;
        this.order = order;
        this.purpose = purpose;
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }

    public String purpose() {
        return purpose;
    }
}
