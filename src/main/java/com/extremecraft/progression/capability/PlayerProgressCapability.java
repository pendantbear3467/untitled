package com.extremecraft.progression.capability;

import net.minecraft.nbt.CompoundTag;

public class PlayerProgressCapability {
    private String playerClass = "warrior";
    private int playerLevel = 1;
    private int skillPoints = 0;
    private boolean magicUnlocked = false;
    private boolean dualWieldUnlocked = true;

    public String playerClass() {
        return playerClass;
    }

    public int playerLevel() {
        return playerLevel;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public boolean magicUnlocked() {
        return magicUnlocked;
    }

    public boolean dualWieldUnlocked() {
        return dualWieldUnlocked;
    }

    public void setPlayerClass(String playerClass) {
        if (playerClass != null && !playerClass.isBlank()) {
            this.playerClass = playerClass;
        }
    }

    public void setPlayerLevel(int playerLevel) {
        this.playerLevel = Math.max(1, playerLevel);
    }

    public void setSkillPoints(int skillPoints) {
        this.skillPoints = Math.max(0, skillPoints);
    }

    public void setMagicUnlocked(boolean magicUnlocked) {
        this.magicUnlocked = magicUnlocked;
    }

    public void setDualWieldUnlocked(boolean dualWieldUnlocked) {
        this.dualWieldUnlocked = dualWieldUnlocked;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("player_class", playerClass);
        tag.putInt("player_level", playerLevel);
        tag.putInt("skill_points", skillPoints);
        tag.putBoolean("magic_unlocked", magicUnlocked);
        tag.putBoolean("dual_wield_unlocked", dualWieldUnlocked);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        setPlayerClass(tag.getString("player_class"));
        setPlayerLevel(tag.getInt("player_level"));
        setSkillPoints(tag.getInt("skill_points"));
        setMagicUnlocked(tag.getBoolean("magic_unlocked"));
        setDualWieldUnlocked(tag.getBoolean("dual_wield_unlocked"));
    }

    public void copyFrom(PlayerProgressCapability other) {
        this.playerClass = other.playerClass;
        this.playerLevel = other.playerLevel;
        this.skillPoints = other.skillPoints;
        this.magicUnlocked = other.magicUnlocked;
        this.dualWieldUnlocked = other.dualWieldUnlocked;
    }
}
