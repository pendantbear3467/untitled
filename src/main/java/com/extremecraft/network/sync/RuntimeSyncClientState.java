package com.extremecraft.network.sync;

import net.minecraft.nbt.CompoundTag;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeSyncClientState {
    private static final Map<String, Double> PLAYER_STATS = new LinkedHashMap<>();
    private static final Map<String, Integer> ABILITY_COOLDOWNS = new LinkedHashMap<>();
    private static final java.util.Set<String> SKILL_UNLOCKS = new java.util.LinkedHashSet<>();
    private static CompoundTag MACHINE_STATES = new CompoundTag();

    private RuntimeSyncClientState() {
    }

    public static void applyStats(CompoundTag tag) {
        PLAYER_STATS.clear();
        for (String key : tag.getAllKeys()) {
            if (tag.contains(key, net.minecraft.nbt.Tag.TAG_DOUBLE)) {
                PLAYER_STATS.put(key, tag.getDouble(key));
            }
        }
    }

    public static void applyAbilities(CompoundTag tag) {
        ABILITY_COOLDOWNS.clear();
        CompoundTag cooldowns = tag.getCompound("cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            ABILITY_COOLDOWNS.put(key, cooldowns.getInt(key));
        }
    }

    public static void applySkillUnlocks(CompoundTag tag) {
        SKILL_UNLOCKS.clear();
        net.minecraft.nbt.ListTag list = tag.getList("skills", net.minecraft.nbt.Tag.TAG_STRING);
        for (net.minecraft.nbt.Tag item : list) {
            SKILL_UNLOCKS.add(item.getAsString());
        }
    }

    public static void applyMachineStates(CompoundTag tag) {
        MACHINE_STATES = tag.copy();
    }

    public static Map<String, Double> playerStats() {
        return Map.copyOf(PLAYER_STATS);
    }

    public static Map<String, Integer> abilityCooldowns() {
        return Map.copyOf(ABILITY_COOLDOWNS);
    }

    public static java.util.Set<String> skillUnlocks() {
        return java.util.Set.copyOf(SKILL_UNLOCKS);
    }

    public static CompoundTag machineStates() {
        return MACHINE_STATES.copy();
    }
}

