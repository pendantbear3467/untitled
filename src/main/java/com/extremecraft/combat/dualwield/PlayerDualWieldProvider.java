package com.extremecraft.combat.dualwield;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerDualWieldProvider implements ICapabilitySerializable<CompoundTag> {
    public static final Capability<PlayerDualWieldData> PLAYER_DUAL_WIELD = CapabilityManager.get(new CapabilityToken<>() {});

    private final PlayerDualWieldData data = new PlayerDualWieldData();
    private final LazyOptional<PlayerDualWieldData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        return cap == PLAYER_DUAL_WIELD ? optional.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return data.serializeNBT();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        data.deserializeNBT(nbt);
    }

    public void invalidate() {
        optional.invalidate();
    }
}

