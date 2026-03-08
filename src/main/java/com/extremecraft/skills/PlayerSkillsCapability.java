package com.extremecraft.skills;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillsCapability {
    private final Map<String, Integer> skillLevels = new HashMap<>();
    private final Map<String, Integer> skillXp = new HashMap<>();

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public int getSkillXp(String skillId) {
        return skillXp.getOrDefault(skillId, 0);
    }

    public void setSkillLevel(String skillId, int level) {
        SkillDefinition def = SkillRegistry.byId(skillId);
        int max = def == null ? Math.max(0, level) : def.maxLevel();
        int clamped = Math.max(0, Math.min(level, max));
        if (clamped == 0) {
            skillLevels.remove(skillId);
            skillXp.remove(skillId);
            return;
        }

        skillLevels.put(skillId, clamped);
        skillXp.put(skillId, 0);
    }

    public void addSkillLevel(String skillId, int amount) {
        if (amount <= 0) {
            return;
        }
        setSkillLevel(skillId, getSkillLevel(skillId) + amount);
    }

    public int addSkillXp(String skillId, int amount) {
        if (skillId == null || skillId.isBlank() || amount <= 0) {
            return 0;
        }

        String normalized = skillId.trim().toLowerCase();
        SkillDefinition def = SkillRegistry.byId(normalized);
        int maxLevel = def == null ? 100 : def.maxLevel();
        int level = Math.max(0, getSkillLevel(normalized));
        if (level >= maxLevel) {
            skillXp.put(normalized, 0);
            return 0;
        }

        int xp = Math.max(0, getSkillXp(normalized)) + amount;
        int gainedLevels = 0;
        while (level < maxLevel) {
            int required = xpForNextLevel(level);
            if (xp < required) {
                break;
            }

            xp -= required;
            level++;
            gainedLevels++;
        }

        skillLevels.put(normalized, level);
        if (level >= maxLevel) {
            skillXp.put(normalized, 0);
        } else {
            skillXp.put(normalized, xp);
        }
        return gainedLevels;
    }

    public static int xpForNextLevel(int currentLevel) {
        int safeLevel = Math.max(0, currentLevel);
        return 20 + ((safeLevel + 1) * 15);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag levelsTag = new CompoundTag();
        skillLevels.forEach(levelsTag::putInt);
        tag.put("levels", levelsTag);
        CompoundTag xpTag = new CompoundTag();
        skillXp.forEach(xpTag::putInt);
        tag.put("xp", xpTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        skillLevels.clear();
        skillXp.clear();
        if (!tag.contains("levels")) {
            return;
        }

        CompoundTag levelsTag = tag.getCompound("levels");
        for (String key : levelsTag.getAllKeys()) {
            skillLevels.put(key, Math.max(0, levelsTag.getInt(key)));
        }

        if (tag.contains("xp")) {
            CompoundTag xpTag = tag.getCompound("xp");
            for (String key : xpTag.getAllKeys()) {
                skillXp.put(key, Math.max(0, xpTag.getInt(key)));
            }
        }
    }

    public void copyFrom(PlayerSkillsCapability other) {
        skillLevels.clear();
        skillLevels.putAll(other.skillLevels);
        skillXp.clear();
        skillXp.putAll(other.skillXp);
    }
}
