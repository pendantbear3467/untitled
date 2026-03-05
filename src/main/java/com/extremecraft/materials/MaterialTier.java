package com.extremecraft.materials;

public enum MaterialTier {
    TIER_1("Tier 1", 1),
    TIER_2("Tier 2", 2),
    TIER_3("Tier 3", 3),
    TIER_4("Tier 4", 4);

    private final String label;
    private final int order;

    MaterialTier(String label, int order) {
        this.label = label;
        this.order = order;
    }

    public String label() {
        return label;
    }

    public int order() {
        return order;
    }
}
