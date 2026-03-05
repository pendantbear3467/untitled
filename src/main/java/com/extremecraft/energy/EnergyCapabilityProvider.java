package com.extremecraft.energy;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EnergyCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
    private final ECPowerStorage storage;
    private final LazyOptional<IEnergyStorage> optional;

    public EnergyCapabilityProvider(ECPowerStorage storage) {
        this.storage = storage;
        this.optional = LazyOptional.of(() -> storage);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return optional.cast();
        }
        return LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("energy", storage.getEnergyStored());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        storage.setStored(nbt.getInt("energy"));
    }

    public void invalidate() {
        optional.invalidate();
    }
}
