package com.extremecraft.progression.capability;

import com.extremecraft.progression.skilltree.SkillModifier;
import com.extremecraft.progression.skilltree.SkillNode;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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

    // Skill-derived gameplay modifiers.
    private float damageBonus = 0.0F;
    private float damageMultiplier = 1.0F;
    private float manaRegenBonus = 0.0F;
    private float staminaCostReductionBonus = 0.0F;
    private float lootRarityBonusSkill = 0.0F;
    private float spellPowerBonus = 0.0F;
    private float attackSpeedBonus = 0.0F;
    private float movementSpeedBonus = 0.0F;
    private float miningSpeedBonus = 0.0F;
    private float blockBreakSpeedBonus = 0.0F;
    private float luckBonus = 0.0F;
    private float maxManaBonus = 0.0F;
    private float maxHealthBonus = 0.0F;
    private float cooldownReductionBonus = 0.0F;

    // Future expansion scaffolding.
    private int ascensionLevel = 0;
    private final Set<String> legendaryPerks = new HashSet<>();
    private final Map<String, Float> equipmentAdditiveModifiers = new HashMap<>();

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

    public int ascensionLevel() { return ascensionLevel; }
    public Set<String> legendaryPerks() { return legendaryPerks; }

    public Set<String> unlockedSkillNodes() { return unlockedSkillNodes; }

    public int maxHealth() {
        return 20 + (vitality * 2) + Math.round(maxHealthBonus);
    }

    public float equipmentModifier(String id) {
        if (id == null || id.isBlank()) {
            return 0.0F;
        }
        return equipmentAdditiveModifiers.getOrDefault(id.trim().toLowerCase(), 0.0F);
    }

    public int meleeDamageBonus() {
        return Math.round(strength + damageBonus);
    }

    public float damageMultiplier() {
        return Math.max(0.1F, damageMultiplier);
    }

    public int heavyWeaponDamageBonus() {
        return Math.max(0, Math.round((strength / 2.0F) + (damageBonus * 0.5F)));
    }

    public float healthRegenBonus() {
        return vitality * 0.04F;
    }

    public float dodgeChance() {
        return agility * 0.0025F;
    }

    public float staminaCostReduction() {
        return Math.min(0.80F, Math.min(0.40F, endurance * 0.006F) + staminaCostReductionBonus);
    }

    public float lootRarityBonus() {
        return (luck * 0.01F) + lootRarityBonusSkill;
    }

    public float spellPowerBonus() {
        return spellPowerBonus;
    }

    public float cooldownReduction() {
        return Math.min(0.80F, Math.max(0.0F, cooldownReductionBonus));
    }

    public float miningSpeedBonus() {
        return miningSpeedBonus;
    }

    public float blockBreakSpeedBonus() {
        return blockBreakSpeedBonus;
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

    public void setLevel(int level) {
        this.level = Math.max(1, level);
        this.experience = 0;
        this.experienceToNextLevel = xpForLevel(this.level);
        recalculateDerivedStats();
    }

    public boolean unlockSkillNode(String nodeId, int skillPointCost) {
        if (nodeId == null || nodeId.isBlank() || skillPointCost <= 0 || skillPoints < skillPointCost) {
            return false;
        }

        if (!unlockedSkillNodes.add(nodeId)) {
            return false;
        }

        skillPoints -= skillPointCost;
        recalculateDerivedStats();
        return true;
    }

    public boolean isSkillUnlocked(String nodeId) {
        return unlockedSkillNodes.contains(nodeId);
    }

    public void setEquipmentModifier(String id, float value) {
        if (id == null || id.isBlank()) {
            return;
        }
        equipmentAdditiveModifiers.put(id, value);
        recalculateDerivedStats();
    }

    public void removeEquipmentModifier(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        equipmentAdditiveModifiers.remove(id);
        recalculateDerivedStats();
    }

    public boolean replaceEquipmentModifiers(Map<String, Float> newModifiers) {
        Map<String, Float> normalized = new HashMap<>();
        if (newModifiers != null) {
            newModifiers.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    normalized.put(key.trim().toLowerCase(), value);
                }
            });
        }

        if (equipmentAdditiveModifiers.equals(normalized)) {
            return false;
        }

        equipmentAdditiveModifiers.clear();
        equipmentAdditiveModifiers.putAll(normalized);
        recalculateDerivedStats();
        return true;
    }

    public void setAscensionLevel(int ascensionLevel) {
        this.ascensionLevel = Math.max(0, ascensionLevel);
        recalculateDerivedStats();
    }

    public void recalculateDerivedStats() {
        // Base primary formulas.
        maxMana = 50 + (intelligence * 5);
        manaCapacity = maxMana;
        magicPower = intelligence * 2;

        attackSpeed = 4.0F + (agility * 0.01F);
        movementSpeed = 0.10F + (agility * 0.01F);

        critChance = luck * 0.002F;
        critDamage = 1.5F + (strength * 0.02F);

        maxStamina = 100 + (endurance * 10);

        // Reset skill-derived modifiers before reapplying from unlocked nodes.
        damageBonus = 0.0F;
        damageMultiplier = 1.0F;
        manaRegenBonus = 0.0F;
        staminaCostReductionBonus = 0.0F;
        lootRarityBonusSkill = 0.0F;
        spellPowerBonus = 0.0F;
        attackSpeedBonus = 0.0F;
        movementSpeedBonus = 0.0F;
        miningSpeedBonus = 0.0F;
        blockBreakSpeedBonus = 0.0F;
        luckBonus = 0.0F;
        maxManaBonus = 0.0F;
        maxHealthBonus = 0.0F;
        cooldownReductionBonus = 0.0F;

        // Apply unlocked skill node modifiers.
        for (String unlockedNode : unlockedSkillNodes) {
            SkillNode node = SkillTreeManager.getNode(unlockedNode);
            if (node == null) {
                continue;
            }

            for (SkillModifier modifier : node.modifiers()) {
                applySkillModifier(modifier);
            }
        }

        // Future equipment modifiers entry point.
        for (Map.Entry<String, Float> entry : equipmentAdditiveModifiers.entrySet()) {
            switch (entry.getKey()) {
                case "damage_bonus" -> damageBonus += entry.getValue();
                case "crit_chance_bonus" -> critChance += entry.getValue();
                case "attack_speed", "attack_speed_bonus" -> attackSpeedBonus += entry.getValue();
                case "max_mana" -> maxManaBonus += entry.getValue();
                case "max_health" -> maxHealthBonus += entry.getValue();
                case "luck" -> luckBonus += entry.getValue();
                case "mining_speed" -> miningSpeedBonus += entry.getValue();
                case "block_break_speed" -> blockBreakSpeedBonus += entry.getValue();
                case "movement_speed" -> movementSpeedBonus += entry.getValue();
                case "movement_speed_bonus" -> movementSpeed += entry.getValue();
                case "spell_power_bonus", "spell_damage" -> spellPowerBonus += entry.getValue();
                case "cooldown_reduction" -> cooldownReductionBonus += entry.getValue();
                case "mana" -> maxManaBonus += entry.getValue();
                case "damage_multiplier" -> damageMultiplier *= Math.max(0.1F, entry.getValue());
                case "stamina_cost_reduction" -> staminaCostReductionBonus += entry.getValue();
                case "mana_regeneration" -> manaRegenBonus += entry.getValue();
                case "crit_damage" -> critDamage += entry.getValue();
                default -> {
                }
            }
        }

        maxMana += Math.round(maxManaBonus);
        manaCapacity = maxMana;
        attackSpeed += attackSpeedBonus;
        movementSpeed += movementSpeedBonus;
        critChance += luckBonus * 0.002F;

        // Future ascension scaling.
        if (ascensionLevel > 0) {
            damageMultiplier *= (1.0F + (ascensionLevel * 0.01F));
            critDamage += ascensionLevel * 0.02F;
            movementSpeed += ascensionLevel * 0.0015F;
        }

        // Apply spell power bonus to magic power.
        magicPower = Math.max(1, Math.round(magicPower * (1.0F + spellPowerBonus)));

        mana = Math.min(mana, maxMana);
        stamina = Math.min(stamina, maxStamina);
    }

    public void regenerateResources() {
        int staminaRegen = Math.max(1, endurance / 3);
        int manaRegen = Math.max(1, intelligence / 4) + Math.max(0, Math.round(manaRegenBonus));

        stamina = Math.min(maxStamina, stamina + staminaRegen);
        mana = Math.min(maxMana, mana + manaRegen);
    }

    public boolean tryConsumeMana(int amount) {
        if (amount <= 0) {
            return true;
        }

        if (mana < amount) {
            return false;
        }

        mana -= amount;
        return true;
    }

    private void applySkillModifier(SkillModifier modifier) {
        String id = modifier.modifierId();
        float value = (float) modifier.value();

        switch (id) {
            case "damage_bonus" -> damageBonus = applyValue(damageBonus, value, modifier.operation());
            case "damage_multiplier" -> damageMultiplier = applyValue(damageMultiplier, value, modifier.operation());
            case "crit_chance_bonus" -> critChance = applyValue(critChance, value, modifier.operation());
            case "attack_speed" -> attackSpeedBonus = applyValue(attackSpeedBonus, value, modifier.operation());
            case "mana_regeneration" -> manaRegenBonus = applyValue(manaRegenBonus, value, modifier.operation());
            case "max_mana" -> maxManaBonus = applyValue(maxManaBonus, value, modifier.operation());
            case "max_health" -> maxHealthBonus = applyValue(maxHealthBonus, value, modifier.operation());
            case "movement_speed" -> movementSpeedBonus = applyValue(movementSpeedBonus, value, modifier.operation());
            case "mining_speed" -> miningSpeedBonus = applyValue(miningSpeedBonus, value, modifier.operation());
            case "luck" -> luckBonus = applyValue(luckBonus, value, modifier.operation());
            case "block_break_speed" -> blockBreakSpeedBonus = applyValue(blockBreakSpeedBonus, value, modifier.operation());
            case "stamina_cost_reduction" -> staminaCostReductionBonus = applyValue(staminaCostReductionBonus, value, modifier.operation());
            case "loot_rarity_bonus" -> lootRarityBonusSkill = applyValue(lootRarityBonusSkill, value, modifier.operation());
            case "spell_power_bonus", "spell_damage" -> spellPowerBonus = applyValue(spellPowerBonus, value, modifier.operation());
            case "cooldown_reduction" -> cooldownReductionBonus = applyValue(cooldownReductionBonus, value, modifier.operation());
            case "mana" -> maxManaBonus = applyValue(maxManaBonus, value, modifier.operation());
            default -> {
            }
        }
    }

    private float applyValue(float current, float value, SkillModifier.Operation operation) {
        return switch (operation) {
            case ADD -> current + value;
            case MULTIPLY -> current * value;
            case PERCENT -> current * (1.0F + value);
        };
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

        tag.putInt("ascensionLevel", ascensionLevel);

        ListTag unlocked = new ListTag();
        for (String node : unlockedSkillNodes) {
            unlocked.add(StringTag.valueOf(node));
        }
        tag.put("unlockedSkillNodes", unlocked);

        ListTag perks = new ListTag();
        for (String perk : legendaryPerks) {
            perks.add(StringTag.valueOf(perk));
        }
        tag.put("legendaryPerks", perks);

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

        ascensionLevel = Math.max(0, tag.getInt("ascensionLevel"));

        unlockedSkillNodes.clear();
        ListTag list = tag.getList("unlockedSkillNodes", Tag.TAG_STRING);
        for (Tag entry : list) {
            unlockedSkillNodes.add(entry.getAsString());
        }

        legendaryPerks.clear();
        ListTag perks = tag.getList("legendaryPerks", Tag.TAG_STRING);
        for (Tag entry : perks) {
            legendaryPerks.add(entry.getAsString());
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

        this.ascensionLevel = other.ascensionLevel;

        this.unlockedSkillNodes.clear();
        this.unlockedSkillNodes.addAll(other.unlockedSkillNodes);

        this.legendaryPerks.clear();
        this.legendaryPerks.addAll(other.legendaryPerks);

        this.equipmentAdditiveModifiers.clear();
        this.equipmentAdditiveModifiers.putAll(other.equipmentAdditiveModifiers);
    }

    public static int xpForLevel(int level) {
        int clamped = Math.max(1, level);
        return clamped * clamped * 10;
    }
}



