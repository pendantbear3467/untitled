package com.extremecraft.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ReactorHeatService {
    private ReactorHeatService() {
    }

    public static TickResult tick(ServerLevel level, BlockPos pos, com.extremecraft.machine.core.TechMachineBlockEntity machine, ReactorControlService.ReactorState state, ReactorMultiblockService.ValidationState structure) {
        int redstoneSignal = Math.max(0, level.getBestNeighborSignal(pos));
        int insertionPercent = state.manualInsertionPercent() >= 0
            ? state.manualInsertionPercent()
            : (int) Math.round((redstoneSignal / 15.0D) * 100.0D);
        insertionPercent = Math.max(0, Math.min(100, insertionPercent));
        state.setControlSignal(insertionPercent);

        double insertion = state.scrammed() ? 1.0D : (insertionPercent / 100.0D);
        double activity = state.fuelTicksRemaining() > 0 ? Math.max(0.08D, 1.0D - (insertion * 0.9D)) : 0.0D;
        double structureScale = Math.max(1.0D, structure.fuelColumns()) * (1.0D + (structure.moderatorParts() * 0.06D));
        double heatGain = state.reactivity() * activity * structureScale * Math.max(0.15D, 1.0D - (insertion * 0.8D));
        double cooling = structure.coolingBonus()
            + (structure.coolantParts() * 3.5D)
            + (structure.shielding() * 0.25D)
            + (state.scrammed() ? 8.0D : 0.0D);
        double nextHeat = Math.max(0.0D, state.heat() + heatGain - cooling);
        double transferScale = Math.max(1.0D, structure.powerParts() * 0.8D);
        int feGenerated = (int) Math.round(Math.max(0.0D, heatGain * 28.0D * transferScale * Math.max(0.25D, 1.0D - (insertion * 0.5D))));

        state.setHeat(nextHeat);
        state.setSteam(Math.max(0.0D, state.steam() + (heatGain * 2.5D) - 1.5D));
        state.setWaste(state.waste() + (activity * 0.08D));
        state.setRadiation(Math.max(0.0D, (state.radiation() * 0.75D) + (state.reactivity() * 0.35D)));
        machine.getEnergyStorageExt().receiveEnergy(feGenerated, false);
        return new TickResult(feGenerated, heatGain, cooling);
    }

    public record TickResult(int feGenerated, double heatGain, double cooling) {
    }
}
