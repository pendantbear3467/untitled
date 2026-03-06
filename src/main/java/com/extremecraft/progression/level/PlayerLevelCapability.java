package com.extremecraft.progression.level;

import net.minecraft.nbt.CompoundTag;

public class PlayerLevelCapability {
    private int level = 1;
    private int xp = 0;
    private int skillPoints = 0;

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public int xpRequiredForCurrentLevel() {
        return xpRequired(level);
    }

    public int grantXp(int amount) {
        if (amount <= 0) {
            return 0;
        }

        xp += amount;
        int levelUps = 0;
        while (xp >= xpRequired(level)) {
            xp -= xpRequired(level);
            level++;
            skillPoints++;
            levelUps++;
        }
        return levelUps;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
        this.xp = 0;
    }

    public boolean consumeSkillPoint() {
        if (skillPoints <= 0) {
            return false;
        }
        skillPoints--;
        return true;
    }

    public void addSkillPoints(int amount) {
        if (amount > 0) {
            skillPoints += amount;
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("skill_points", skillPoints);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        xp = Math.max(0, tag.getInt("xp"));
        skillPoints = Math.max(0, tag.getInt("skill_points"));
    }

    public void copyFrom(PlayerLevelCapability other) {
        this.level = other.level;
        this.xp = other.xp;
        this.skillPoints = other.skillPoints;
    }

    public static int xpRequired(int level) {
        int safe = Math.max(1, level);
        return safe * safe * 10;
    }
}
