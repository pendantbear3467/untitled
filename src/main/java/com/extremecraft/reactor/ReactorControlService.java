package com.extremecraft.reactor;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.dev.validation.ECTickProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class ReactorControlService {
    private static final String DATA_NAME = "extremecraft_reactor_control";

    private ReactorControlService() {
    }

    public static boolean isReactorController(String machineId) {
        return ReactorIdentity.isFirstReleaseReactor(machineId);
    }

    public static CompoundTag snapshot(Level level, BlockPos pos) {
        CompoundTag tag = new CompoundTag();
        if (!(level instanceof ServerLevel serverLevel) || pos == null) {
            return tag;
        }

        ReactorState state = data(serverLevel).states.get(pos.asLong());
        if (state == null) {
            return tag;
        }

        tag.putDouble("heat", state.heat());
        tag.putDouble("steam", state.steam());
        tag.putDouble("waste", state.waste());
        tag.putDouble("reactivity", state.reactivity());
        tag.putDouble("radiation", state.radiation());
        tag.putInt("fuel_ticks_remaining", state.fuelTicksRemaining());
        tag.putInt("control_signal", state.controlSignal());
        tag.putBoolean("scrammed", state.scrammed());
        tag.putBoolean("melted_down", state.meltedDown());
        return tag;
    }

    public static boolean tickController(Level level, BlockPos pos, com.extremecraft.machine.core.TechMachineBlockEntity machine) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        long start = System.nanoTime();
        ReactorControlData data = data(serverLevel);
        ReactorState state = data.state(pos);
        ReactorMultiblockService.ValidationState structure = ReactorMultiblockService.validate(serverLevel, pos);
        boolean changed = false;

        if (!structure.valid()) {
            if (!state.scrammed()) {
                state.setScrammed(true);
                changed = true;
            }
            double cooled = Math.max(0.0D, state.heat() - 5.0D);
            if (cooled != state.heat()) {
                state.setHeat(cooled);
                changed = true;
            }
            double radiation = Math.max(0.0D, state.radiation() - 0.2D);
            if (radiation != state.radiation()) {
                state.setRadiation(radiation);
                changed = true;
            }
        } else if (!state.meltedDown()) {
            changed |= ReactorFuelService.refuel(machine, state);
            changed |= ReactorFuelService.tickBurn(state);
            ReactorHeatService.TickResult tick = ReactorHeatService.tick(serverLevel, pos, machine, state, structure);
            changed |= tick.feGenerated() > 0 || tick.heatGain() > 0.0D || tick.cooling() > 0.0D;
            changed |= ReactorSafetyService.applySafety(serverLevel, pos, machine, state);

            if (state.scrammed() && state.heat() < (ECFoundationConfig.reactorScramHeatThreshold() * 0.35D) && serverLevel.getBestNeighborSignal(pos) <= 0) {
                state.setScrammed(false);
                changed = true;
            }
        }

        if (changed) {
            data.setDirty();
        }
        if (ECFoundationConfig.isProfilerEnabled()) {
            ECTickProfiler.record("reactor_tick", System.nanoTime() - start);
        }
        return changed;
    }

    public static double sampleAmbientRadiation(ServerLevel level, BlockPos pos, int radius) {
        ReactorControlData data = data(level);
        double radiation = 0.0D;
        int radiusSq = radius * radius;
        for (Map.Entry<Long, ReactorState> entry : data.states.entrySet()) {
            BlockPos controllerPos = BlockPos.of(entry.getKey());
            if (controllerPos.distSqr(pos) > radiusSq) {
                continue;
            }

            ReactorState state = entry.getValue();
            radiation += state.meltedDown() ? 12.0D : 0.0D;
            radiation += state.radiation() * 0.35D;
            radiation += state.heat() / 500.0D;
        }
        return radiation;
    }

    private static ReactorControlData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ReactorControlData::load, ReactorControlData::new, DATA_NAME);
    }

    private static final class ReactorControlData extends SavedData {
        private final Map<Long, ReactorState> states = new HashMap<>();

        private static ReactorControlData load(CompoundTag tag) {
            ReactorControlData data = new ReactorControlData();
            ListTag list = tag.getList("reactors", Tag.TAG_COMPOUND);
            for (Tag entry : list) {
                if (!(entry instanceof CompoundTag reactorTag)) {
                    continue;
                }
                data.states.put(reactorTag.getLong("pos"), ReactorState.load(reactorTag.getCompound("state")));
            }
            return data;
        }

        private ReactorState state(BlockPos pos) {
            return states.computeIfAbsent(pos.asLong(), ignored -> new ReactorState());
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag list = new ListTag();
            for (Map.Entry<Long, ReactorState> entry : states.entrySet()) {
                CompoundTag reactorTag = new CompoundTag();
                reactorTag.putLong("pos", entry.getKey());
                reactorTag.put("state", entry.getValue().save());
                list.add(reactorTag);
            }
            tag.put("reactors", list);
            return tag;
        }
    }

    public static final class ReactorState {
        private double heat;
        private double steam;
        private double waste;
        private double reactivity;
        private double radiation;
        private int fuelTicksRemaining;
        private int controlSignal;
        private boolean scrammed;
        private boolean meltedDown;

        private static ReactorState load(CompoundTag tag) {
            ReactorState state = new ReactorState();
            state.heat = tag.getDouble("heat");
            state.steam = tag.getDouble("steam");
            state.waste = tag.getDouble("waste");
            state.reactivity = tag.getDouble("reactivity");
            state.radiation = tag.getDouble("radiation");
            state.fuelTicksRemaining = tag.getInt("fuel_ticks_remaining");
            state.controlSignal = tag.getInt("control_signal");
            state.scrammed = tag.getBoolean("scrammed");
            state.meltedDown = tag.getBoolean("melted_down");
            return state;
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putDouble("heat", heat);
            tag.putDouble("steam", steam);
            tag.putDouble("waste", waste);
            tag.putDouble("reactivity", reactivity);
            tag.putDouble("radiation", radiation);
            tag.putInt("fuel_ticks_remaining", fuelTicksRemaining);
            tag.putInt("control_signal", controlSignal);
            tag.putBoolean("scrammed", scrammed);
            tag.putBoolean("melted_down", meltedDown);
            return tag;
        }

        public double heat() { return heat; }
        public void setHeat(double value) { heat = Math.max(0.0D, value); }
        public double steam() { return steam; }
        public void setSteam(double value) { steam = Math.max(0.0D, value); }
        public double waste() { return waste; }
        public void setWaste(double value) { waste = Math.max(0.0D, value); }
        public double reactivity() { return reactivity; }
        public void setReactivity(double value) { reactivity = Math.max(0.0D, value); }
        public double radiation() { return radiation; }
        public void setRadiation(double value) { radiation = Math.max(0.0D, value); }
        public int fuelTicksRemaining() { return fuelTicksRemaining; }
        public void setFuelTicksRemaining(int value) { fuelTicksRemaining = Math.max(0, value); }
        public int controlSignal() { return controlSignal; }
        public void setControlSignal(int value) { controlSignal = Math.max(0, value); }
        public boolean scrammed() { return scrammed; }
        public void setScrammed(boolean value) { scrammed = value; }
        public boolean meltedDown() { return meltedDown; }
        public void setMeltedDown(boolean value) { meltedDown = value; }
    }
}
