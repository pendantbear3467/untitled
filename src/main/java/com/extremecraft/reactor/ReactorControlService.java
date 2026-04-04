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
import java.util.function.Consumer;

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
        tag.putInt("manual_insertion_percent", state.manualInsertionPercent());
        tag.putInt("generation_per_tick", state.generationPerTick());
        tag.putInt("waste_capacity", state.wasteCapacity());
        tag.putInt("fuel_columns", state.fuelColumns());
        tag.putInt("size_x", state.sizeX());
        tag.putInt("size_y", state.sizeY());
        tag.putInt("size_z", state.sizeZ());
        tag.putInt("power_ports", state.powerPorts());
        tag.putBoolean("assembled", state.assembled());
        tag.putBoolean("active", state.active());
        tag.putBoolean("scrammed", state.scrammed());
        tag.putBoolean("melted_down", state.meltedDown());
        tag.putString("validation_reason", state.validationReason());
        tag.putString("warning", state.warning());
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

        changed |= state.setAssembled(structure.valid());
        changed |= state.setValidationReason(structure.reason());
        changed |= state.setSizeX(structure.sizeX());
        changed |= state.setSizeY(structure.sizeY());
        changed |= state.setSizeZ(structure.sizeZ());
        changed |= state.setFuelColumns(structure.fuelColumns());
        changed |= state.setPowerPorts(structure.powerParts());
        changed |= state.setWasteCapacity(Math.max(500, structure.fuelColumns() * 700));

        if (!structure.valid()) {
            if (!state.scrammed()) {
                state.setScrammed(true);
                changed = true;
            }
            if (state.active()) {
                state.setActive(false);
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
            changed |= state.setGenerationPerTick(0);
            changed |= state.setWarning("Structure invalid");
        } else if (!state.meltedDown()) {
            if (!state.active()) {
                changed |= state.setGenerationPerTick(0);
                double cooled = Math.max(0.0D, state.heat() - 3.0D);
                if (cooled != state.heat()) {
                    state.setHeat(cooled);
                    changed = true;
                }
                double radiation = Math.max(0.0D, state.radiation() - 0.1D);
                if (radiation != state.radiation()) {
                    state.setRadiation(radiation);
                    changed = true;
                }
                changed |= state.setWarning("Offline");
            } else {
                changed |= ReactorFuelService.refuel(machine, state);
                changed |= ReactorFuelService.tickBurn(state);
                ReactorHeatService.TickResult tick = ReactorHeatService.tick(serverLevel, pos, machine, state, structure);
                changed |= tick.feGenerated() > 0 || tick.heatGain() > 0.0D || tick.cooling() > 0.0D;
                changed |= state.setGenerationPerTick(tick.feGenerated());
                changed |= ReactorSafetyService.applySafety(serverLevel, pos, machine, state);

                if (state.waste() >= state.wasteCapacity()) {
                    if (!state.scrammed()) {
                        state.setScrammed(true);
                        changed = true;
                    }
                    if (state.active()) {
                        state.setActive(false);
                        changed = true;
                    }
                    changed |= state.setWarning("Waste buffer full");
                } else if (state.scrammed()) {
                    changed |= state.setWarning("SCRAM engaged");
                } else if (state.fuelTicksRemaining() <= 0) {
                    changed |= state.setWarning("Refueling");
                } else {
                    changed |= state.setWarning("Nominal");
                }
            }

            if (state.scrammed()
                    && state.heat() < (ECFoundationConfig.reactorScramHeatThreshold() * 0.35D)
                    && serverLevel.getBestNeighborSignal(pos) <= 0
                    && state.active()) {
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

    public static boolean setActive(ServerLevel level, BlockPos pos, boolean active) {
        return mutateState(level, pos, state -> {
            if (state.meltedDown()) {
                return;
            }
            if (active) {
                state.setActive(true);
                state.setScrammed(false);
                state.setWarning("Booting");
            } else {
                state.setActive(false);
                state.setWarning("Offline");
            }
        });
    }

    public static boolean setManualInsertion(ServerLevel level, BlockPos pos, int percent) {
        int clamped = Math.max(0, Math.min(100, percent));
        return mutateState(level, pos, state -> {
            state.setManualInsertionPercent(clamped);
            if (clamped >= 100 && !state.scrammed()) {
                state.setScrammed(true);
            } else if (clamped < 100 && state.active() && state.scrammed() && state.heat() < (ECFoundationConfig.reactorScramHeatThreshold() * 0.35D)) {
                state.setScrammed(false);
            }
        });
    }

    public static boolean scram(ServerLevel level, BlockPos pos) {
        return mutateState(level, pos, state -> {
            state.setScrammed(true);
            state.setActive(false);
            state.setManualInsertionPercent(100);
            state.setWarning("SCRAM engaged");
        });
    }

    private static boolean mutateState(ServerLevel level, BlockPos pos, Consumer<ReactorState> mutation) {
        if (level == null || pos == null || mutation == null) {
            return false;
        }

        ReactorControlData data = data(level);
        ReactorState state = data.state(pos);
        ReactorState before = state.copy();
        mutation.accept(state);
        if (!state.equalsState(before)) {
            data.setDirty();
            return true;
        }
        return false;
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
        private int manualInsertionPercent = 0;
        private int generationPerTick;
        private int wasteCapacity = 500;
        private int fuelColumns;
        private int sizeX;
        private int sizeY;
        private int sizeZ;
        private int powerPorts;
        private boolean assembled;
        private boolean active;
        private boolean scrammed;
        private boolean meltedDown;
        private String validationReason = "Unknown";
        private String warning = "Offline";

        private static ReactorState load(CompoundTag tag) {
            ReactorState state = new ReactorState();
            state.heat = tag.getDouble("heat");
            state.steam = tag.getDouble("steam");
            state.waste = tag.getDouble("waste");
            state.reactivity = tag.getDouble("reactivity");
            state.radiation = tag.getDouble("radiation");
            state.fuelTicksRemaining = tag.getInt("fuel_ticks_remaining");
            state.controlSignal = tag.getInt("control_signal");
            state.manualInsertionPercent = tag.getInt("manual_insertion_percent");
            state.generationPerTick = tag.getInt("generation_per_tick");
            state.wasteCapacity = Math.max(1, tag.getInt("waste_capacity"));
            state.fuelColumns = Math.max(0, tag.getInt("fuel_columns"));
            state.sizeX = Math.max(0, tag.getInt("size_x"));
            state.sizeY = Math.max(0, tag.getInt("size_y"));
            state.sizeZ = Math.max(0, tag.getInt("size_z"));
            state.powerPorts = Math.max(0, tag.getInt("power_ports"));
            state.assembled = tag.getBoolean("assembled");
            state.active = tag.getBoolean("active");
            state.scrammed = tag.getBoolean("scrammed");
            state.meltedDown = tag.getBoolean("melted_down");
            state.validationReason = tag.getString("validation_reason");
            state.warning = tag.getString("warning");
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
            tag.putInt("manual_insertion_percent", manualInsertionPercent);
            tag.putInt("generation_per_tick", generationPerTick);
            tag.putInt("waste_capacity", wasteCapacity);
            tag.putInt("fuel_columns", fuelColumns);
            tag.putInt("size_x", sizeX);
            tag.putInt("size_y", sizeY);
            tag.putInt("size_z", sizeZ);
            tag.putInt("power_ports", powerPorts);
            tag.putBoolean("assembled", assembled);
            tag.putBoolean("active", active);
            tag.putBoolean("scrammed", scrammed);
            tag.putBoolean("melted_down", meltedDown);
            tag.putString("validation_reason", validationReason == null ? "" : validationReason);
            tag.putString("warning", warning == null ? "" : warning);
            return tag;
        }

        private ReactorState copy() {
            ReactorState copy = new ReactorState();
            copy.heat = heat;
            copy.steam = steam;
            copy.waste = waste;
            copy.reactivity = reactivity;
            copy.radiation = radiation;
            copy.fuelTicksRemaining = fuelTicksRemaining;
            copy.controlSignal = controlSignal;
            copy.manualInsertionPercent = manualInsertionPercent;
            copy.generationPerTick = generationPerTick;
            copy.wasteCapacity = wasteCapacity;
            copy.fuelColumns = fuelColumns;
            copy.sizeX = sizeX;
            copy.sizeY = sizeY;
            copy.sizeZ = sizeZ;
            copy.powerPorts = powerPorts;
            copy.assembled = assembled;
            copy.active = active;
            copy.scrammed = scrammed;
            copy.meltedDown = meltedDown;
            copy.validationReason = validationReason;
            copy.warning = warning;
            return copy;
        }

        private boolean equalsState(ReactorState other) {
            if (other == null) {
                return false;
            }
            return Double.compare(heat, other.heat) == 0
                    && Double.compare(steam, other.steam) == 0
                    && Double.compare(waste, other.waste) == 0
                    && Double.compare(reactivity, other.reactivity) == 0
                    && Double.compare(radiation, other.radiation) == 0
                    && fuelTicksRemaining == other.fuelTicksRemaining
                    && controlSignal == other.controlSignal
                    && manualInsertionPercent == other.manualInsertionPercent
                    && generationPerTick == other.generationPerTick
                    && wasteCapacity == other.wasteCapacity
                    && fuelColumns == other.fuelColumns
                    && sizeX == other.sizeX
                    && sizeY == other.sizeY
                    && sizeZ == other.sizeZ
                    && powerPorts == other.powerPorts
                    && assembled == other.assembled
                    && active == other.active
                    && scrammed == other.scrammed
                    && meltedDown == other.meltedDown
                    && safe(validationReason).equals(safe(other.validationReason))
                    && safe(warning).equals(safe(other.warning));
        }

        private static String safe(String value) {
            return value == null ? "" : value;
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
        public int manualInsertionPercent() { return manualInsertionPercent; }
        public void setManualInsertionPercent(int value) { manualInsertionPercent = Math.max(0, Math.min(100, value)); }
        public int generationPerTick() { return generationPerTick; }
        public boolean setGenerationPerTick(int value) {
            int next = Math.max(0, value);
            if (generationPerTick == next) {
                return false;
            }
            generationPerTick = next;
            return true;
        }
        public int wasteCapacity() { return wasteCapacity; }
        public boolean setWasteCapacity(int value) {
            int next = Math.max(1, value);
            if (wasteCapacity == next) {
                return false;
            }
            wasteCapacity = next;
            return true;
        }
        public int fuelColumns() { return fuelColumns; }
        public boolean setFuelColumns(int value) {
            int next = Math.max(0, value);
            if (fuelColumns == next) {
                return false;
            }
            fuelColumns = next;
            return true;
        }
        public int sizeX() { return sizeX; }
        public boolean setSizeX(int value) {
            int next = Math.max(0, value);
            if (sizeX == next) {
                return false;
            }
            sizeX = next;
            return true;
        }
        public int sizeY() { return sizeY; }
        public boolean setSizeY(int value) {
            int next = Math.max(0, value);
            if (sizeY == next) {
                return false;
            }
            sizeY = next;
            return true;
        }
        public int sizeZ() { return sizeZ; }
        public boolean setSizeZ(int value) {
            int next = Math.max(0, value);
            if (sizeZ == next) {
                return false;
            }
            sizeZ = next;
            return true;
        }
        public int powerPorts() { return powerPorts; }
        public boolean setPowerPorts(int value) {
            int next = Math.max(0, value);
            if (powerPorts == next) {
                return false;
            }
            powerPorts = next;
            return true;
        }
        public boolean assembled() { return assembled; }
        public boolean setAssembled(boolean value) {
            if (assembled == value) {
                return false;
            }
            assembled = value;
            return true;
        }
        public boolean active() { return active; }
        public void setActive(boolean value) { active = value; }
        public boolean scrammed() { return scrammed; }
        public void setScrammed(boolean value) { scrammed = value; }
        public boolean meltedDown() { return meltedDown; }
        public void setMeltedDown(boolean value) { meltedDown = value; }
        public String validationReason() { return safe(validationReason); }
        public boolean setValidationReason(String value) {
            String next = safe(value);
            if (validationReason().equals(next)) {
                return false;
            }
            validationReason = next;
            return true;
        }
        public String warning() { return safe(warning); }
        public boolean setWarning(String value) {
            String next = safe(value);
            if (warning().equals(next)) {
                return false;
            }
            warning = next;
            return true;
        }
    }
}
