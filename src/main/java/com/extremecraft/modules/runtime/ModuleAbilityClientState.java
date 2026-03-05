package com.extremecraft.modules.runtime;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModuleAbilityClientState {
    private static final Map<String, Integer> COOLDOWNS = new LinkedHashMap<>();

    private ModuleAbilityClientState() {
    }

    public static void applySync(CompoundTag tag) {
        COOLDOWNS.clear();
        CompoundTag cooldownTag = tag.getCompound("cooldowns");
        for (String key : cooldownTag.getAllKeys()) {
            COOLDOWNS.put(key, Math.max(0, cooldownTag.getInt(key)));
        }
    }

    public static int cooldownRemaining(String abilityId) {
        return COOLDOWNS.getOrDefault(abilityId, 0);
    }
}
