package com.extremecraft.machine.core;

import net.minecraftforge.energy.IEnergyStorage;

public interface IECEStorage extends IEnergyStorage {
    default int getECEStored() {
        return getEnergyStored();
    }

    default int getMaxECEStored() {
        return getMaxEnergyStored();
    }
}
