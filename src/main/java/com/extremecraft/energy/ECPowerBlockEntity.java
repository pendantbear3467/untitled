package com.extremecraft.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ECPowerBlockEntity extends BlockEntity {
    protected final ECPowerStorage powerStorage;

    protected ECPowerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, int maxReceive, int maxExtract) {
        super(type, pos, state);
        this.powerStorage = new ECPowerStorage(capacity, maxReceive, maxExtract);
    }

    public ECPowerStorage getPowerStorage() {
        return powerStorage;
    }

    protected boolean consumePower(int amount) {
        return powerStorage.tryConsume(amount);
    }

    protected boolean hasPower(int amount) {
        return powerStorage.hasEnergy(amount);
    }
}
