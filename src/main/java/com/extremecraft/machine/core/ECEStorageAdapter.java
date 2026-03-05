package com.extremecraft.machine.core;

import com.extremecraft.energy.EnergyStorageExt;

public class ECEStorageAdapter implements IECEStorage {
    private final EnergyStorageExt delegate;

    public ECEStorageAdapter(EnergyStorageExt delegate) {
        this.delegate = delegate;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return delegate.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return delegate.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int getEnergyStored() {
        return delegate.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return delegate.canExtract();
    }

    @Override
    public boolean canReceive() {
        return delegate.canReceive();
    }
}
