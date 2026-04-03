package com.extremecraft.combat;

/**
 * Shared armor mitigation formula for the combat pipeline.
 */
public final class ArmorCalculator {
    private ArmorCalculator() {
    }

    /**
     * Converts armor points into a normalized damage reduction ratio.
     */
    public static float calculateReduction(float armor) {
        float safeArmor = Math.max(0.0F, armor);
        return safeArmor / (safeArmor + 100.0F);
    }
}
