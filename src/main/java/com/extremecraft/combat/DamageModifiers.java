package com.extremecraft.combat;

public final class DamageModifiers {
    private float skillFlatBonus = 0.0F;
    private float skillMultiplier = 1.0F;

    private float weaponFlatBonus = 0.0F;
    private float weaponMultiplier = 1.0F;

    private float abilityFlatBonus = 0.0F;
    private float abilityMultiplier = 1.0F;

    private float criticalChanceBonus = 0.0F;
    private float criticalMultiplierBonus = 0.0F;

    private float armorMultiplier = 1.0F;
    private float resistanceMultiplier = 1.0F;
    private float statusMultiplier = 1.0F;

    public float skillFlatBonus() {
        return skillFlatBonus;
    }

    public void addSkillFlatBonus(float value) {
        this.skillFlatBonus += value;
    }

    public float skillMultiplier() {
        return skillMultiplier;
    }

    public void multiplySkill(float value) {
        this.skillMultiplier *= Math.max(0.0F, value);
    }

    public float weaponFlatBonus() {
        return weaponFlatBonus;
    }

    public void addWeaponFlatBonus(float value) {
        this.weaponFlatBonus += value;
    }

    public float weaponMultiplier() {
        return weaponMultiplier;
    }

    public void multiplyWeapon(float value) {
        this.weaponMultiplier *= Math.max(0.0F, value);
    }

    public float abilityFlatBonus() {
        return abilityFlatBonus;
    }

    public void addAbilityFlatBonus(float value) {
        this.abilityFlatBonus += value;
    }

    public float abilityMultiplier() {
        return abilityMultiplier;
    }

    public void multiplyAbility(float value) {
        this.abilityMultiplier *= Math.max(0.0F, value);
    }

    public float criticalChanceBonus() {
        return criticalChanceBonus;
    }

    public void addCriticalChanceBonus(float value) {
        this.criticalChanceBonus += value;
    }

    public float criticalMultiplierBonus() {
        return criticalMultiplierBonus;
    }

    public void addCriticalMultiplierBonus(float value) {
        this.criticalMultiplierBonus += value;
    }

    public float armorMultiplier() {
        return armorMultiplier;
    }

    public void multiplyArmor(float value) {
        this.armorMultiplier *= Math.max(0.0F, value);
    }

    public float resistanceMultiplier() {
        return resistanceMultiplier;
    }

    public void multiplyResistance(float value) {
        this.resistanceMultiplier *= Math.max(0.0F, value);
    }

    public float statusMultiplier() {
        return statusMultiplier;
    }

    public void multiplyStatus(float value) {
        this.statusMultiplier *= Math.max(0.0F, value);
    }
}
