package com.extremecraft.skills;

import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.Map;

public class PlayerSkillsCapability {
    private final Map<String, Integer> skillLevels = new HashMap<>();

    public int getSkillLevel(String skillId) {
        return skillLevels.getOrDefault(skillId, 0);
    }

    public void setSkillLevel(String skillId, int level) {
        SkillDefinition def = SkillRegistry.byId(skillId);
        int max = def == null ? Math.max(0, level) : def.maxLevel();
        int clamped = Math.max(0, Math.min(level, max));
        if (clamped == 0) {
            skillLevels.remove(skillId);
            return;
        }

        skillLevels.put(skillId, clamped);
    }

    public void addSkillLevel(String skillId, int amount) {
        if (amount <= 0) {
            return;
        }
        setSkillLevel(skillId, getSkillLevel(skillId) + amount);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag levelsTag = new CompoundTag();
        skillLevels.forEach(levelsTag::putInt);
        tag.put("levels", levelsTag);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        skillLevels.clear();
        if (!tag.contains("levels")) {
            return;
        }

        CompoundTag levelsTag = tag.getCompound("levels");
        for (String key : levelsTag.getAllKeys()) {
            skillLevels.put(key, Math.max(0, levelsTag.getInt(key)));
        }
    }

    public void copyFrom(PlayerSkillsCapability other) {
        skillLevels.clear();
        skillLevels.putAll(other.skillLevels);
    }
}
