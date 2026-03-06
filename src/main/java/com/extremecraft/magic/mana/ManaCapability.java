package com.extremecraft.magic.mana;

import net.minecraft.nbt.CompoundTag;

public class ManaCapability {
    private double currentMana = 100.0D;
    private double maxMana = 100.0D;
    private double manaRegen = 0.08D;
    private double spellPower = 1.0D;
    private double regenAccumulator = 0.0D;

    public double currentMana() {
        return currentMana;
    }

    public double maxMana() {
        return maxMana;
    }

    public double manaRegen() {
        return manaRegen;
    }

    public double spellPower() {
        return spellPower;
    }

    public void setCurrentMana(double value) {
        currentMana = clamp(value, 0.0D, maxMana);
    }

    public void setMaxMana(double value) {
        maxMana = Math.max(1.0D, value);
        currentMana = Math.min(currentMana, maxMana);
    }

    public void setManaRegen(double value) {
        manaRegen = Math.max(0.0D, value);
    }

    public void setSpellPower(double value) {
        spellPower = Math.max(0.1D, value);
    }

    public void applyDerivedStats(double nextMaxMana, double nextManaRegen, double nextSpellPower) {
        double prevMax = Math.max(1.0D, maxMana);
        double ratio = currentMana / prevMax;

        maxMana = Math.max(1.0D, nextMaxMana);
        manaRegen = Math.max(0.0D, nextManaRegen);
        spellPower = Math.max(0.1D, nextSpellPower);
        currentMana = clamp(maxMana * ratio, 0.0D, maxMana);
    }

    public boolean regenerateTick() {
        if (currentMana >= maxMana || manaRegen <= 0.0D) {
            return false;
        }

        regenAccumulator += manaRegen;
        if (regenAccumulator < 1.0D) {
            return false;
        }

        int gained = (int) regenAccumulator;
        regenAccumulator -= gained;

        double before = currentMana;
        currentMana = Math.min(maxMana, currentMana + gained);
        return currentMana > before;
    }

    public boolean consume(double amount) {
        if (amount <= 0.0D) {
            return true;
        }

        if (currentMana < amount) {
            return false;
        }

        currentMana -= amount;
        return true;
    }

    public void refill() {
        currentMana = maxMana;
        regenAccumulator = 0.0D;
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("current_mana", currentMana);
        tag.putDouble("max_mana", maxMana);
        tag.putDouble("mana_regen", manaRegen);
        tag.putDouble("spell_power", spellPower);
        tag.putDouble("regen_accumulator", regenAccumulator);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        maxMana = Math.max(1.0D, tag.getDouble("max_mana"));
        currentMana = clamp(tag.getDouble("current_mana"), 0.0D, maxMana);
        manaRegen = Math.max(0.0D, tag.getDouble("mana_regen"));
        spellPower = Math.max(0.1D, tag.getDouble("spell_power"));
        regenAccumulator = Math.max(0.0D, tag.getDouble("regen_accumulator"));
    }

    public void copyFrom(ManaCapability other) {
        this.currentMana = other.currentMana;
        this.maxMana = other.maxMana;
        this.manaRegen = other.manaRegen;
        this.spellPower = other.spellPower;
        this.regenAccumulator = other.regenAccumulator;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
