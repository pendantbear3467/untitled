package com.extremecraft.progression.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashSet;
import java.util.Set;

public class PlayerStatsCapability {
    private int level = 1;
    private int experience = 0;
    private int experienceToNextLevel = xpForLevel(1);

    private int statPoints = 0;
    private int skillPoints = 0;

    private int vitality = 1;
    private int strength = 1;
    private int agility = 1;
    private int endurance = 1;
    private int intelligence = 1;
    private int luck = 1;

    private int manaCapacity = 55;
    private int magicPower = 2;
    private float attackSpeed = 4.01F;
    private float movementSpeed = 0.11F;
    private float critChance = 0.002F;
    private float critDamage = 1.52F;

    private int stamina = 110;
    private int maxStamina = 110;
    private int mana = 55;
    private int maxMana = 55;

    private final Set<String> unlockedSkillNodes = new HashSet<>();

    public int level() { return level; }
    public int experience() { return experience; }
    public int experienceToNextLevel() { return experienceToNextLevel; }
    public int statPoints() { return statPoints; }
    public int skillPoints() { return skillPoints; }

    public int vitality() { return vitality; }
    public int strength() { return strength; }
    public int agility() { return agility; }
    public int endurance() { return endurance; }
    public int intelligence() { return intelligence; }
    public int luck() { return luck; }

    public int manaCapacity() { return manaCapacity; }
    public int magicPower() { return magicPower; }
    public float attackSpeed() { return attackSpeed; }
    public float movementSpeed() { return movementSpeed; }
    public float critChance() { return critChance; }
    public float critDamage() { return critDamage; }

    public int stamina() { return stamina; }
    public int maxStamina() { return maxStamina; }
    public int mana() { return mana; }
    public int maxMana() { return maxMana; }

    public Set<String> unlockedSkillNodes() { return unlockedSkillNodes; }

    public int maxHealth() {
        return 20 + (vitality * 2);
    }

    public int meleeDamageBonus() {
        return strength;
    }

    public int heavyWeaponDamageBonus() {
        return Math.max(0, strength / 2);
    }

    public float healthRegenBonus() {
        return vitality * 0.04F;
    }

    public float dodgeChance() {
        return agility * 0.0025F;
    }

    public float staminaCostReduction() {
        return Math.min(0.40F, endurance * 0.006F);
    }

    public float lootRarityBonus() {
        return luck * 0.01F;
    }

    public boolean addExperience(int amount) {
        if (amount <= 0) {
            return false;
        }

        experience += amount;
        boolean leveled = false;

        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            statPoints += 3;
            skillPoints += 1;
            experienceToNextLevel = xpForLevel(level);
            leveled = true;
        }

        if (leveled) {
            recalculateDerivedStats();
        }

        return leveled;
    }

    public boolean upgradePrimaryStat(String statId) {
        if (statPoints <= 0 || statId == null || statId.isBlank()) {
            return false;
        }

        switch (statId.trim().toLowerCase()) {
            case "vitality" -> vitality++;
            case "strength" -> strength++;
            case "agility" -> agility++;
            case "endurance" -> endurance++;
            case "intelligence" -> intelligence++;
            case "luck" -> luck++;
            default -> {
                return false;
            }
        }

        statPoints--;
        recalculateDerivedStats();
        return true;
    }

    public boolean unlockSkillNode(String nodeId, int skillPointCost) {
        if (nodeId == null || nodeId.isBlank() || skillPointCost <= 0 || skillPoints < skillPointCost) {
            return false;
        }

        if (!unlockedSkillNodes.add(nodeId)) {
            return false;
        }

        skillPoints -= skillPointCost;
        return true;
    }

    public boolean isSkillUnlocked(String nodeId) {
        return unlockedSkillNodes.contains(nodeId);
    }

    public void recalculateDerivedStats() {
        maxMana = 50 + (intelligence * 5);
        manaCapacity = maxMana;
        magicPower = intelligence * 2;

        attackSpeed = 4.0F + (agility * 0.01F);
        movementSpeed = 0.10F + (agility * 0.01F);

        critChance = luck * 0.002F;
        critDamage = 1.5F + (strength * 0.02F);

        maxStamina = 100 + (endurance * 10);

        mana = Math.min(mana, maxMana);
        stamina = Math.min(stamina, maxStamina);
    }

    public void regenerateResources() {
        stamina = Math.min(maxStamina, stamina + Math.max(1, endurance / 3));
        mana = Math.min(maxMana, mana + Math.max(1, intelligence / 4));
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putInt("experience", experience);
        tag.putInt("experienceToNextLevel", experienceToNextLevel);
        tag.putInt("statPoints", statPoints);
        tag.putInt("skillPoints", skillPoints);

        tag.putInt("vitality", vitality);
        tag.putInt("strength", strength);
        tag.putInt("agility", agility);
        tag.putInt("endurance", endurance);
        tag.putInt("intelligence", intelligence);
        tag.putInt("luck", luck);

        tag.putInt("manaCapacity", manaCapacity);
        tag.putInt("magicPower", magicPower);
        tag.putFloat("attackSpeed", attackSpeed);
        tag.putFloat("movementSpeed", movementSpeed);
        tag.putFloat("critChance", critChance);
        tag.putFloat("critDamage", critDamage);

        tag.putInt("stamina", stamina);
        tag.putInt("maxStamina", maxStamina);
        tag.putInt("mana", mana);
        tag.putInt("maxMana", maxMana);

        ListTag unlocked = new ListTag();
        for (String node : unlockedSkillNodes) {
            unlocked.add(StringTag.valueOf(node));
        }
        tag.put("unlockedSkillNodes", unlocked);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        level = Math.max(1, tag.getInt("level"));
        experience = Math.max(0, tag.getInt("experience"));
        experienceToNextLevel = Math.max(1, tag.getInt("experienceToNextLevel"));
        statPoints = Math.max(0, tag.getInt("statPoints"));
        skillPoints = Math.max(0, tag.getInt("skillPoints"));

        vitality = Math.max(1, tag.getInt("vitality"));
        strength = Math.max(1, tag.getInt("strength"));
        agility = Math.max(1, tag.getInt("agility"));
        endurance = Math.max(1, tag.getInt("endurance"));
        intelligence = Math.max(1, tag.getInt("intelligence"));
        luck = Math.max(1, tag.getInt("luck"));

        mana = Math.max(0, tag.getInt("mana"));
        stamina = Math.max(0, tag.getInt("stamina"));

        unlockedSkillNodes.clear();
        ListTag list = tag.getList("unlockedSkillNodes", Tag.TAG_STRING);
        for (Tag entry : list) {
            unlockedSkillNodes.add(entry.getAsString());
        }

        recalculateDerivedStats();
    }

    public void copyFrom(PlayerStatsCapability other) {
        this.level = other.level;
        this.experience = other.experience;
        this.experienceToNextLevel = other.experienceToNextLevel;
        this.statPoints = other.statPoints;
        this.skillPoints = other.skillPoints;

        this.vitality = other.vitality;
        this.strength = other.strength;
        this.agility = other.agility;
        this.endurance = other.endurance;
        this.intelligence = other.intelligence;
        this.luck = other.luck;

        this.manaCapacity = other.manaCapacity;
        this.magicPower = other.magicPower;
        this.attackSpeed = other.attackSpeed;
        this.movementSpeed = other.movementSpeed;
        this.critChance = other.critChance;
        this.critDamage = other.critDamage;

        this.stamina = other.stamina;
        this.maxStamina = other.maxStamina;
        this.mana = other.mana;
        this.maxMana = other.maxMana;

        this.unlockedSkillNodes.clear();
        this.unlockedSkillNodes.addAll(other.unlockedSkillNodes);
    }

    public static int xpForLevel(int level) {
        return 100 + (Math.max(1, level) * 25);
    }
}
