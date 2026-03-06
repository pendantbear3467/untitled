package com.extremecraft.combat;

public final class ArmorCalculator {
    private ArmorCalculator() {
    }

    public static float calculateReduction(float armor) {
        float safeArmor = Math.max(0.0F, armor);
        return safeArmor / (safeArmor + 100.0F);
    }
}
