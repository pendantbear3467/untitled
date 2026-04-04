package com.extremecraft.ecosystem.core.progression;

import java.util.function.ToIntBiFunction;

public final class ProgressionSkillLevelBridge {
    private static volatile ToIntBiFunction<Object, String> levelProvider = (carrier, skillId) -> 0;

    private ProgressionSkillLevelBridge() {
    }

    public static void setProvider(ToIntBiFunction<Object, String> provider) {
        levelProvider = provider == null ? (player, skillId) -> 0 : provider;
    }

    public static int skillLevel(Object carrier, String skillId) {
        return levelProvider.applyAsInt(carrier, skillId);
    }
}