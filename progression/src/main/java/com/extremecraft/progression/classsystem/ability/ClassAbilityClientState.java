package com.extremecraft.progression.classsystem.ability;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;

/**
 * Client-side mirror of class ability cooldowns for UI/tooltips.
 */
public final class ClassAbilityClientState {
    private static final Map<String, Integer> COOLDOWNS = new LinkedHashMap<>();

    private ClassAbilityClientState() {
    }

    public static synchronized void applySync(CompoundTag tag) {
        COOLDOWNS.clear();
        if (tag == null) {
            return;
        }

        CompoundTag cooldowns = tag.getCompound("cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            COOLDOWNS.put(key, Math.max(0, cooldowns.getInt(key)));
        }
    }

    public static synchronized int cooldownTicks(String abilityId) {
        return COOLDOWNS.getOrDefault(abilityId, 0);
    }

    public static synchronized void tickDown() {
        COOLDOWNS.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
    }
}
