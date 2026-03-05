package com.extremecraft.machine.cable;

public enum CableTier {
    COPPER("copper_cable", 120),
    GOLD("gold_cable", 420),
    SUPERCONDUCTIVE("superconductive_cable", 1800);

    private final String id;
    private final int transferPerTick;

    CableTier(String id, int transferPerTick) {
        this.id = id;
        this.transferPerTick = transferPerTick;
    }

    public String id() {
        return id;
    }

    public int transferPerTick() {
        return transferPerTick;
    }
}
