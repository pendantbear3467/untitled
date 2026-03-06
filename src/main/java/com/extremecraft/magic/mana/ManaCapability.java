package com.extremecraft.magic.mana;

import net.minecraft.nbt.CompoundTag;

public class ManaCapability {
    private final ManaStorage storage = new ManaStorage(100.0D, 100.0D, 0.08D);
    private double spellPower = 1.0D;

    public double currentMana() {
        return storage.currentMana();
    }

    public double maxMana() {
        return storage.maxMana();
    }

    public double manaRegen() {
        return storage.manaRegen();
    }

    public double spellPower() {
        return spellPower;
    }

    public ManaStorage storage() {
        return storage;
    }

    public void setCurrentMana(double value) {
        storage.setCurrentMana(value);
    }

    public void setMaxMana(double value) {
        storage.setMaxMana(value);
    }

    public void setManaRegen(double value) {
        storage.setManaRegen(value);
    }

    public void setSpellPower(double value) {
        spellPower = Math.max(0.1D, value);
    }

    public void applyDerivedStats(double nextMaxMana, double nextManaRegen, double nextSpellPower) {
        storage.applyDerived(nextMaxMana, nextManaRegen);
        spellPower = Math.max(0.1D, nextSpellPower);
    }

    public boolean regenerateTick() {
        return storage.regenerateTick();
    }

    public boolean consume(double amount) {
        return storage.consume(amount);
    }

    public void refill() {
        storage.refill();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = storage.serializeNBT();
        tag.putDouble("spell_power", spellPower);
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        storage.deserializeNBT(tag);
        spellPower = Math.max(0.1D, tag.getDouble("spell_power"));
    }

    public void copyFrom(ManaCapability other) {
        this.storage.copyFrom(other.storage);
        this.spellPower = other.spellPower;
    }
}
