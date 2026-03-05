package com.extremecraft.progression.capability;

import net.minecraft.nbt.CompoundTag;

public class PlayerStatsCapability {
    private String playerClass = "warrior";
    private int skillPoints = 0;
    private int strength = 1;
    private int agility = 1;
    private int magic = 1;
    private int defense = 1;
    private boolean dualWieldUnlocked = false;
    private boolean magicUnlocked = false;

    public String playerClass() {
        return playerClass;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public int strength() {
        return strength;
    }

    public int agility() {
        return agility;
    }

    public int magic() {
        return magic;
    }

    public int defense() {
        return defense;
    }

    public boolean dualWieldUnlocked() {
        return dualWieldUnlocked;
    }

    public boolean magicUnlocked() {
        return magicUnlocked;
    }

    public void setPlayerClass(String playerClass) {
        if (playerClass != null && !playerClass.isBlank()) {
            this.playerClass = playerClass;
        }
    }

    public void addSkillPoints(int amount) {
        if (amount > 0) {
            this.skillPoints += amount;
        }
    }

    public boolean upgrade(String statId) {
        if (skillPoints <= 0 || statId == null) {
            return false;
        }

        switch (statId.trim().toLowerCase()) {
            case "strength" -> strength++;
            case "agility" -> agility++;
            case "magic" -> magic++;
            case "defense" -> defense++;
            default -> {
                return false;
            }
        }

        skillPoints--;
        evaluateUnlocks();
        return true;
    }

    public void evaluateUnlocks() {
        dualWieldUnlocked = strength >= 3 || agility >= 3;
        magicUnlocked = magic >= 3;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("playerClass", playerClass);
        tag.putInt("skillPoints", skillPoints);
        tag.putInt("strength", strength);
        tag.putInt("agility", agility);
        tag.putInt("magic", magic);
        tag.putInt("defense", defense);
        tag.putBoolean("dualWieldUnlocked", dualWieldUnlocked);
        tag.putBoolean("magicUnlocked", magicUnlocked);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        playerClass = tag.getString("playerClass");
        skillPoints = Math.max(0, tag.getInt("skillPoints"));
        strength = Math.max(1, tag.getInt("strength"));
        agility = Math.max(1, tag.getInt("agility"));
        magic = Math.max(1, tag.getInt("magic"));
        defense = Math.max(1, tag.getInt("defense"));
        dualWieldUnlocked = tag.getBoolean("dualWieldUnlocked");
        magicUnlocked = tag.getBoolean("magicUnlocked");
        evaluateUnlocks();
    }

    public void copyFrom(PlayerStatsCapability other) {
        this.playerClass = other.playerClass;
        this.skillPoints = other.skillPoints;
        this.strength = other.strength;
        this.agility = other.agility;
        this.magic = other.magic;
        this.defense = other.defense;
        this.dualWieldUnlocked = other.dualWieldUnlocked;
        this.magicUnlocked = other.magicUnlocked;
    }
}
