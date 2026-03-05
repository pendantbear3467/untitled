package com.extremecraft.energy;

public class ECPowerStorage extends EnergyStorageExt {
    public ECPowerStorage(int capacity, int maxReceive, int maxExtract) {
        super(capacity, maxReceive, maxExtract);
    }

    public boolean hasEnergy(int amount) {
        return getEnergyStored() >= Math.max(0, amount);
    }

    public boolean tryConsume(int amount) {
        return consume(Math.max(0, amount));
    }
}
