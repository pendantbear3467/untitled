package com.extremecraft.endgame;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.dev.validation.ECTickProfiler;
import com.extremecraft.foundation.ECDestructiveEffectService;
import com.extremecraft.magic.AetherNetworkService;
import com.extremecraft.platform.data.definition.EndgameCoreDefinition;
import com.extremecraft.platform.data.registry.EndgameCoreDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

public final class EndgameCoreStructureService {
    private static final String DATA_NAME = "extremecraft_endgame_core_state";
    private static final Map<ControllerKey, ValidationState> CACHE = new ConcurrentHashMap<>();

    private EndgameCoreStructureService() {
    }

    public static boolean isCoreController(String machineId) {
        return "dimensional_reactor".equals(machineId);
    }

    public static boolean tickController(Level level, BlockPos pos, com.extremecraft.machine.core.TechMachineBlockEntity machine) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        long start = System.nanoTime();
        ValidationState validation = validate(serverLevel, pos);
        CoreStateData data = data(serverLevel);
        CoreState state = data.state(pos);
        boolean changed = false;

        int aetherCost = Math.max(6, validation.totalThroughputBonus() / 60);
        if (validation.valid() && AetherNetworkService.tryConsume(serverLevel, pos, aetherCost)) {
            int generation = Math.max(40, 40 + (validation.totalEnergyBonus() / 40));
            machine.getEnergyStorageExt().receiveEnergy(generation, false);
            state.setOvercharge(Math.max(0.0D, state.overcharge() + (machine.getEnergyStorageExt().getEnergyStored() >= machine.getEnergyStorageExt().getMaxEnergyStored() ? 2.0D : 0.5D)));
            state.setInstability(Math.max(0.0D, state.instability() - Math.max(0.5D, validation.totalStabilityBonus() * 0.05D)));
            changed = true;
        } else {
            state.setInstability(state.instability() + (validation.valid() ? 1.5D : 2.5D));
            state.setOvercharge(Math.max(0.0D, state.overcharge() - 1.0D));
            changed = true;
        }

        if (state.overcharge() > ECFoundationConfig.endgameMaxInstability()) {
            ECDestructiveEffectService.queueSphere(serverLevel, pos, 8, 96, "endgame_core_overcharge");
            state.setOvercharge(0.0D);
            state.setInstability(Math.min(ECFoundationConfig.endgameMaxInstability(), state.instability() + 12.0D));
            changed = true;
        }

        if (changed) {
            data.setDirty();
        }
        if (ECFoundationConfig.isProfilerEnabled()) {
            ECTickProfiler.record("endgame_core_tick", System.nanoTime() - start);
        }
        return changed;
    }

    public static ValidationState validate(ServerLevel level, BlockPos controllerPos) {
        ControllerKey key = new ControllerKey(level.dimension().location().toString(), controllerPos.immutable());
        return CACHE.computeIfAbsent(key, ignored -> computeValidation(level, controllerPos));
    }

    public static void invalidateAround(Level level, BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }

        int range = Math.max(6, ECFoundationConfig.structureInvalidationRange());
        int maxDistance = range * range;
        String dimension = level.dimension().location().toString();
        CACHE.keySet().removeIf(key -> key.dimension.equals(dimension) && key.controllerPos.distSqr(changedPos) <= maxDistance);
    }

    private static ValidationState computeValidation(ServerLevel level, BlockPos controllerPos) {
        EndgameCoreDefinition definition = activeDefinition();
        int totalEnergyBonus = 0;
        int totalThroughputBonus = 0;
        double totalStabilityBonus = 0.0D;
        String stageId = "core_only";
        boolean anyStage = false;

        for (EndgameCoreDefinition.StageRequirement stage : definition.stages()) {
            boolean complete = true;
            for (EndgameCoreDefinition.BlockRequirement block : stage.blocks()) {
                BlockPos targetPos = controllerPos.offset(block.x(), block.y(), block.z());
                String found = BuiltInRegistries.BLOCK.getKey(level.getBlockState(targetPos).getBlock()).toString();
                if (!block.blockId().equals(found)) {
                    if (!block.optional()) {
                        complete = false;
                    }
                }
            }

            if (!complete) {
                break;
            }

            anyStage = true;
            stageId = stage.stageId();
            totalEnergyBonus += stage.energyBonus();
            totalThroughputBonus += stage.throughputBonus();
            totalStabilityBonus += stage.stabilityBonus();
        }

        return new ValidationState(anyStage, stageId, totalEnergyBonus, totalThroughputBonus, totalStabilityBonus);
    }

    private static EndgameCoreDefinition activeDefinition() {
        for (EndgameCoreDefinition definition : EndgameCoreDataRegistry.registry().all()) {
            return definition;
        }

        List<EndgameCoreDefinition.StageRequirement> fallback = new ArrayList<>();
        fallback.add(new EndgameCoreDefinition.StageRequirement("core_only", 0, 4000, 80, 10.0D, List.of()));
        fallback.add(new EndgameCoreDefinition.StageRequirement("stabilizer", 3, 12000, 220, 22.0D, List.of(
                new EndgameCoreDefinition.BlockRequirement("extremecraft:draconium_block", 3, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:draconium_block", -3, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:draconium_block", 0, 0, 3, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:draconium_block", 0, 0, -3, false)
        )));
        fallback.add(new EndgameCoreDefinition.StageRequirement("ring_shell", 4, 30000, 420, 30.0D, List.of(
                new EndgameCoreDefinition.BlockRequirement("extremecraft:aetherium_block", 4, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:aetherium_block", -4, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:aetherium_block", 0, 0, 4, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:aetherium_block", 0, 0, -4, false)
        )));
        fallback.add(new EndgameCoreDefinition.StageRequirement("pylons", 6, 52000, 760, 38.0D, List.of(
                new EndgameCoreDefinition.BlockRequirement("extremecraft:void_reactor", 6, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:void_reactor", -6, 0, 0, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:void_reactor", 0, 0, 6, false),
                new EndgameCoreDefinition.BlockRequirement("extremecraft:void_reactor", 0, 0, -6, false)
        )));
        return new EndgameCoreDefinition("foundation_core", "extremecraft:dimensional_reactor", List.copyOf(fallback));
    }

    private static CoreStateData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(CoreStateData::load, CoreStateData::new, DATA_NAME);
    }

    public record ValidationState(boolean valid, String stageId, int totalEnergyBonus, int totalThroughputBonus, double totalStabilityBonus) {
    }

    private record ControllerKey(String dimension, BlockPos controllerPos) {
    }

    private static final class CoreStateData extends SavedData {
        private final Map<Long, CoreState> states = new HashMap<>();

        private static CoreStateData load(CompoundTag tag) {
            CoreStateData data = new CoreStateData();
            ListTag list = tag.getList("cores", Tag.TAG_COMPOUND);
            for (Tag element : list) {
                if (!(element instanceof CompoundTag entry)) {
                    continue;
                }
                data.states.put(entry.getLong("pos"), CoreState.load(entry.getCompound("state")));
            }
            return data;
        }

        private CoreState state(BlockPos pos) {
            return states.computeIfAbsent(pos.asLong(), ignored -> new CoreState());
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, CoreState> entry : states.entrySet()) {
                CompoundTag compound = new CompoundTag();
                compound.putLong("pos", entry.getKey());
                compound.put("state", entry.getValue().save());
                list.add(compound);
            }
            tag.put("cores", list);
            return tag;
        }
    }

    private static final class CoreState {
        private double instability;
        private double overcharge;

        private static CoreState load(CompoundTag tag) {
            CoreState state = new CoreState();
            state.instability = tag.getDouble("instability");
            state.overcharge = tag.getDouble("overcharge");
            return state;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("instability", instability);
            tag.putDouble("overcharge", overcharge);
            return tag;
        }

        public double instability() { return instability; }
        public void setInstability(double value) { instability = Math.max(0.0D, value); }
        public double overcharge() { return overcharge; }
        public void setOvercharge(double value) { overcharge = Math.max(0.0D, value); }
    }
}
