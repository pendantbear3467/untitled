package com.extremecraft.machine.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class ECEnergyService {
    private ECEnergyService() {
    }

    public static boolean pushEnergyToNeighbors(Level level, BlockPos pos, TechMachineBlockEntity machine, int maxTransferPerSide) {
        if (level == null || pos == null || machine == null || maxTransferPerSide <= 0 || machine.getEnergyStorageExt().getEnergyStored() <= 0) {
            return false;
        }

        boolean changed = false;
        for (Direction direction : Direction.values()) {
            BlockEntity targetBe = level.getBlockEntity(pos.relative(direction));
            if (targetBe == null) {
                continue;
            }

            IEnergyStorage target = targetBe.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) {
                continue;
            }

            int extracted = machine.getEnergyStorageExt().extractEnergy(maxTransferPerSide, true);
            if (extracted <= 0) {
                continue;
            }

            int accepted = target.receiveEnergy(extracted, false);
            if (accepted > 0) {
                machine.getEnergyStorageExt().extractEnergy(accepted, false);
                changed = true;
            }

            if (machine.getEnergyStorageExt().getEnergyStored() <= 0) {
                break;
            }
        }
        return changed;
    }
}
