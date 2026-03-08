package com.extremecraft.machine.core;

import com.extremecraft.config.Config;
import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.dev.validation.ECTickProfiler;
import com.extremecraft.endgame.EndgameCoreStructureService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class MachineTickScheduler {
    private MachineTickScheduler() {
    }

    public static void serverTick(Level level, BlockPos pos, TechMachineBlockEntity machine) {
        if (level == null || level.isClientSide || machine == null) {
            return;
        }

        if (!Config.isMachineEnabled(machine.getMachineId())) {
            return;
        }

        int tickInterval = Config.machineTickInterval();
        if (tickInterval > 1 && ((level.getGameTime() + pos.asLong()) % tickInterval) != 0L) {
            return;
        }

        long start = System.nanoTime();
        MachineDefinition definition = machine.getMachineDefinition();
        boolean changed;
        if (EndgameCoreStructureService.isCoreController(machine.getMachineId())) {
            changed = EndgameCoreStructureService.tickController(level, pos, machine);
        } else if (definition.category() == MachineCategory.GENERATOR) {
            changed = GeneratorLogic.tickGenerator(level, pos, machine, definition);
        } else {
            changed = MachineProcessingService.tickProcessor(level, pos, machine, definition);
        }

        changed |= ECEnergyService.pushEnergyToNeighbors(level, pos, machine, Config.neighborEnergyPushPerSide());
        if (changed) {
            machine.setChanged();
        }

        if (ECFoundationConfig.isProfilerEnabled()) {
            ECTickProfiler.record("machine_tick", System.nanoTime() - start);
        }
    }
}
