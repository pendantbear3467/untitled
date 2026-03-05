package com.extremecraft.machine.core;

public final class ECEnergy {
    public static final int FE_PER_ECE = 1;

    public static int toECE(int fe) {
        return fe / FE_PER_ECE;
    }

    public static int toFE(int ece) {
        return ece * FE_PER_ECE;
    }

    private ECEnergy() {
    }
}
