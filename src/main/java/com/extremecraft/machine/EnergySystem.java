package com.extremecraft.machine;

import com.extremecraft.machine.core.IECEStorage;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergySystem {
    public enum Kind {
        FE,
        EC
    }

    private EnergySystem() {
    }

    public static boolean consume(IEnergyStorage fe, IECEStorage ec, int amount, Kind preferred) {
        int requested = Math.max(0, amount);
        if (requested == 0) {
            return true;
        }

        if (preferred == Kind.EC) {
            if (consumeEc(ec, requested)) {
                return true;
            }
            return consumeFe(fe, requested);
        }

        if (consumeFe(fe, requested)) {
            return true;
        }
        return consumeEc(ec, requested);
    }

    public static int stored(IEnergyStorage fe, IECEStorage ec) {
        return Math.max(fe == null ? 0 : fe.getEnergyStored(), ec == null ? 0 : ec.getECEStored());
    }

    private static boolean consumeFe(IEnergyStorage storage, int amount) {
        return storage != null && storage.extractEnergy(amount, false) >= amount;
    }

    private static boolean consumeEc(IECEStorage storage, int amount) {
        return storage != null && storage.extractEnergy(amount, false) >= amount;
    }
}
