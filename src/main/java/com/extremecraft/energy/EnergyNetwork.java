package com.extremecraft.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyNetwork {
    private EnergyNetwork() {
    }

    public static void registerProducer(Level level, BlockPos pos) {
        EnergyNetworkManager.registerProducer(level, pos);
    }

    public static void registerConsumer(Level level, BlockPos pos) {
        EnergyNetworkManager.registerConsumer(level, pos);
    }

    public static void registerCable(Level level, BlockPos pos) {
        EnergyNetworkManager.registerCable(level, pos);
    }

    public static void unregister(Level level, BlockPos pos) {
        EnergyNetworkManager.unregister(level, pos);
    }

    public static void tick(Level level) {
        EnergyNetworkManager.tick(level);
    }

    public static int distribute(BlockEntity source, int maxPerSide) {
        if (source.getLevel() == null) {
            return 0;
        }

        IEnergyStorage sourceStorage = source.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        if (sourceStorage == null) {
            return 0;
        }

        int moved = 0;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = source.getLevel().getBlockEntity(source.getBlockPos().relative(direction));
            if (neighbor == null) {
                continue;
            }

            IEnergyStorage target = neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).orElse(null);
            if (target == null || !target.canReceive()) {
                continue;
            }

            int simulated = sourceStorage.extractEnergy(maxPerSide, true);
            if (simulated <= 0) {
                continue;
            }

            int accepted = target.receiveEnergy(simulated, false);
            if (accepted > 0) {
                sourceStorage.extractEnergy(accepted, false);
                moved += accepted;
            }
        }

        return moved;
    }
}
