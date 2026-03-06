package com.extremecraft.progression;

public final class LevelScaling {
    private LevelScaling() {
    }

    public static int xpForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return safeLevel * safeLevel * 10;
    }

    public static double statBudgetForLevel(int level) {
        int safeLevel = Math.max(1, level);
        return 5.0D + (safeLevel * 1.75D);
    }

    public static double levelPowerMultiplier(int level) {
        int safeLevel = Math.max(1, level);
        return 1.0D + ((safeLevel - 1) * 0.035D);
    }
}

