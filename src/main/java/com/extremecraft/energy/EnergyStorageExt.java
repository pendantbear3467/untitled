package com.extremecraft.energy;

import net.minecraftforge.energy.EnergyStorage;

public class EnergyStorageExt extends EnergyStorage {
    public EnergyStorageExt(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public void setStored(int value) {
        this.energy = Math.max(0, Math.min(value, getMaxEnergyStored()));
    }

    public boolean consume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }

        extractEnergy(amount, false);
        return true;
    }

    public int produce(int amount) {
        return receiveEnergy(Math.max(0, amount), false);
    }
}
