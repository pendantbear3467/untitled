package com.extremecraft.machine.cable;

import com.extremecraft.config.Config;
import com.extremecraft.energy.EnergyStorageExt;
import com.extremecraft.future.registry.TechBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class CableBlockEntity extends BlockEntity {
    private final EnergyStorageExt energy = new EnergyStorageExt(40_000, 4_000, 4_000);
    private LazyOptional<IEnergyStorage> energyCap = LazyOptional.empty();

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(TechBlockEntities.CABLE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        energyCap = LazyOptional.of(() -> energy);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energyCap.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CableBlockEntity cable) {
        if (level.isClientSide || !Config.areMachinesEnabled()) {
            return;
        }

        int interval = Config.cableTickInterval();
        if (interval > 1 && ((level.getGameTime() + pos.asLong()) % interval) != 0L) {
            return;
        }

        CableTier tier = CableTier.COPPER;
        if (state.getBlock() instanceof CableBlock cableBlock) {
            tier = cableBlock.tier();
        }

        int transferBudget = tier.transferPerTick();
        for (Direction direction : Direction.values()) {
            if (transferBudget <= 0) {
                break;
            }

            BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
            if (neighbor == null) {
                continue;
            }

            transferBudget -= cable.pullEnergyFrom(neighbor, direction.getOpposite(), transferBudget);
            if (transferBudget <= 0) {
                break;
            }
            transferBudget -= cable.pushEnergyTo(neighbor, direction.getOpposite(), transferBudget);
        }
    }

    private int pullEnergyFrom(BlockEntity neighbor, Direction side, int maxAmount) {
        if (maxAmount <= 0 || energy.getEnergyStored() >= energy.getMaxEnergyStored()) {
            return 0;
        }

        IEnergyStorage source = neighbor.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
        if (source == null || !source.canExtract()) {
            return 0;
        }

        int maxReceivable = energy.receiveEnergy(maxAmount, true);
        int extracted = source.extractEnergy(maxReceivable, false);
        if (extracted > 0) {
            energy.receiveEnergy(extracted, false);
        }

        return extracted;
    }

    private int pushEnergyTo(BlockEntity neighbor, Direction side, int maxAmount) {
        if (maxAmount <= 0 || energy.getEnergyStored() <= 0) {
            return 0;
        }

        IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, side).orElse(null);
        if (target == null || !target.canReceive()) {
            return 0;
        }

        int extracted = energy.extractEnergy(Math.min(maxAmount, energy.getEnergyStored()), true);
        int accepted = target.receiveEnergy(extracted, false);
        if (accepted > 0) {
            energy.extractEnergy(accepted, false);
        }

        return accepted;
    }
}

