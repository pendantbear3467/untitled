package com.extremecraft.reactor;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.foundation.ECDestructiveEffectService;
import com.extremecraft.radiation.RadiationService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ReactorSafetyService {
    private ReactorSafetyService() {
    }

    public static boolean applySafety(ServerLevel level, BlockPos pos, com.extremecraft.machine.core.TechMachineBlockEntity machine, ReactorControlService.ReactorState state) {
        boolean changed = false;
        if (!state.scrammed() && state.heat() >= ECFoundationConfig.reactorScramHeatThreshold()) {
            state.setScrammed(true);
            changed = true;
        }

        if (!state.meltedDown() && state.heat() >= ECFoundationConfig.reactorMaxHeat()) {
            triggerMeltdown(level, pos, machine, state);
            changed = true;
        }
        return changed;
    }

    public static void triggerMeltdown(ServerLevel level, BlockPos pos, com.extremecraft.machine.core.TechMachineBlockEntity machine, ReactorControlService.ReactorState state) {
        state.setMeltedDown(true);
        state.setScrammed(true);
        state.setFuelTicksRemaining(0);
        state.setReactivity(0.0D);
        machine.getEnergyStorageExt().extractEnergy(machine.getEnergyStorageExt().getEnergyStored(), false);
        RadiationService.releaseMeltdown(level, pos, Math.max(80.0D, state.heat() / 2.0D), ECFoundationConfig.reactorMeltdownRadius());
        ECDestructiveEffectService.queueSphere(level, pos, ECFoundationConfig.reactorMeltdownRadius(), ECFoundationConfig.reactorMeltdownBlockBudget(), "reactor_meltdown");
    }
}
