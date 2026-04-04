package com.extremecraft.reactor;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ReactorPartDefinition;
import com.extremecraft.platform.data.registry.ReactorPartDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ReactorMultiblockService {
    private static final String MODID = "extremecraft";
    private static final String CONTROLLER_ID = MODID + ":fusion_reactor";
    private static final String CONTROLLER_COMPAT_ID = MODID + ":fission_reactor";
    private static final int MIN_OUTER = 3;
    private static final int MAX_OUTER = 11;
    private static final Map<ControllerKey, CacheEntry> CACHE = new ConcurrentHashMap<>();

    private ReactorMultiblockService() {
    }

    public static ValidationState validate(ServerLevel level, BlockPos controllerPos) {
        ControllerKey key = new ControllerKey(level.dimension().location().toString(), controllerPos.immutable());
        long now = level.getGameTime();
        CacheEntry cached = CACHE.get(key);
        if (cached != null && cached.expiresAtGameTick() >= now) {
            return cached.state();
        }

        ValidationState computed = computeValidation(level, controllerPos);
        long ttl = Math.max(1L, ECFoundationConfig.reactorValidationIntervalTicks());
        CACHE.put(key, new CacheEntry(computed, now + ttl));
        return computed;
    }

    public static void invalidateAround(Level level, BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }

        int range = Math.max(MAX_OUTER + 1, ECFoundationConfig.structureInvalidationRange());
        int maxDist = range * range;
        String dimension = level.dimension().location().toString();
        CACHE.keySet().removeIf(key -> key.dimension.equals(dimension) && key.controllerPos.distSqr(changedPos) <= maxDist);
    }

    public static void clearDimensionCache(Level level) {
        if (level == null) {
            return;
        }

        String dimension = level.dimension().location().toString();
        CACHE.keySet().removeIf(key -> key.dimension.equals(dimension));
    }

    private static ValidationState computeValidation(ServerLevel level, BlockPos controllerPos) {
        Map<String, ReactorPartDefinition> partsByBlockId = partsByBlockId();
        if (partsByBlockId.isEmpty()) {
            return ValidationState.invalid("No reactor part definitions loaded");
        }

        BlockState controllerState = level.getBlockState(controllerPos);
        String controllerBlockId = BuiltInRegistries.BLOCK.getKey(controllerState.getBlock()).toString();
        if (!isControllerBlock(controllerBlockId)) {
            return ValidationState.invalid("Controller is not a reactor controller block");
        }

        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        open.add(controllerPos.immutable());

        int minX = controllerPos.getX();
        int minY = controllerPos.getY();
        int minZ = controllerPos.getZ();
        int maxX = controllerPos.getX();
        int maxY = controllerPos.getY();
        int maxZ = controllerPos.getZ();

        while (!open.isEmpty()) {
            BlockPos current = open.removeFirst();
            long key = current.asLong();
            if (!visited.add(key)) {
                continue;
            }

            if (Math.abs(current.getX() - controllerPos.getX()) > MAX_OUTER
                    || Math.abs(current.getY() - controllerPos.getY()) > MAX_OUTER
                    || Math.abs(current.getZ() - controllerPos.getZ()) > MAX_OUTER) {
                continue;
            }

            BlockState state = level.getBlockState(current);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            if (!isBoundaryCandidateBlock(blockId, partsByBlockId)) {
                continue;
            }

            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (Direction direction : Direction.values()) {
                open.add(current.relative(direction));
            }
        }

        if (!visited.contains(controllerPos.asLong())) {
            return ValidationState.invalid("Controller is not connected to a reactor shell");
        }

        int sizeX = (maxX - minX) + 1;
        int sizeY = (maxY - minY) + 1;
        int sizeZ = (maxZ - minZ) + 1;
        if (sizeX < MIN_OUTER || sizeY < MIN_OUTER || sizeZ < MIN_OUTER) {
            return ValidationState.invalid("Invalid dimensions: minimum reactor size is 3x3x3");
        }
        if (sizeX > MAX_OUTER || sizeY > MAX_OUTER || sizeZ > MAX_OUTER) {
            return ValidationState.invalid("Invalid dimensions: maximum reactor size is 11x11x11");
        }

        if (!isBoundary(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ(), minX, minY, minZ, maxX, maxY, maxZ)) {
            return ValidationState.invalid("Controller must be placed on the reactor shell");
        }

        int structureParts = 0;
        int moderatorParts = 0;
        int controlParts = 0;
        int coolantParts = 0;
        int wasteParts = 0;
        int powerParts = 0;
        int accessParts = 0;
        int redstoneParts = 0;
        int fuelColumns = 0;

        int controllerCount = 0;
        double cooling = 0.0D;
        double shielding = 0.0D;
        double heatCapacity = 0.0D;

        Map<ColumnKey, ColumnSpan> fuelRodColumns = new HashMap<>();
        for (BlockPos scanPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = level.getBlockState(scanPos);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            ReactorPartDefinition part = partsByBlockId.get(blockId);
            String role = normalizeRole(part == null ? "" : part.role());
            boolean boundary = isBoundary(scanPos.getX(), scanPos.getY(), scanPos.getZ(), minX, minY, minZ, maxX, maxY, maxZ);

            if (part != null) {
                heatCapacity += part.heatCapacity();
                cooling += part.cooling();
                shielding += part.shielding();
            }

            if (isControllerBlock(blockId)) {
                controllerCount++;
            }

            if (boundary) {
                if (!isBoundaryCandidateBlock(blockId, partsByBlockId)) {
                    return ValidationState.invalid("Boundary contains invalid block: " + blockId);
                }

                structureParts++;
                if (hasRole(role, "control")) {
                    controlParts++;
                }
                if (hasRole(role, "coolant")) {
                    coolantParts++;
                }
                if (hasRole(role, "waste")) {
                    wasteParts++;
                }
                if (hasRole(role, "power")) {
                    powerParts++;
                }
                if (hasRole(role, "access")) {
                    accessParts++;
                }
                if (hasRole(role, "redstone")) {
                    redstoneParts++;
                }
                continue;
            }

            // Interior validation.
            if (state.isAir()) {
                continue;
            }

            if (hasRole(role, "fuel")) {
                ColumnKey key = new ColumnKey(scanPos.getX(), scanPos.getZ());
                fuelRodColumns.computeIfAbsent(key, ignored -> new ColumnSpan(scanPos.getY(), scanPos.getY())).include(scanPos.getY());
                continue;
            }

            if (hasRole(role, "moderator")) {
                moderatorParts++;
                continue;
            }

            return ValidationState.invalid("Interior contains invalid block: " + blockId);
        }

        if (controllerCount != 1) {
            return ValidationState.invalid("Reactor shell must contain exactly one controller");
        }
        if (coolantParts < 1) {
            return ValidationState.invalid("Reactor is missing a coolant port");
        }
        if (wasteParts < 1) {
            return ValidationState.invalid("Reactor is missing a waste port");
        }
        if (powerParts < 1) {
            return ValidationState.invalid("Reactor is missing a power tap");
        }
        if (accessParts < 1) {
            return ValidationState.invalid("Reactor is missing an access port");
        }
        if (fuelRodColumns.isEmpty()) {
            return ValidationState.invalid("Reactor has no fuel rod columns");
        }

        int interiorMinY = minY + 1;
        int interiorMaxY = maxY - 1;
        for (Map.Entry<ColumnKey, ColumnSpan> entry : fuelRodColumns.entrySet()) {
            ColumnKey key = entry.getKey();
            ColumnSpan span = entry.getValue();
            if (span.minY < interiorMinY || span.maxY > interiorMaxY) {
                return ValidationState.invalid("Fuel rod column crosses the shell boundary");
            }

            for (int y = span.minY; y <= span.maxY; y++) {
                BlockPos rodPos = new BlockPos(key.x(), y, key.z());
                String blockId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(rodPos).getBlock()).toString();
                ReactorPartDefinition part = partsByBlockId.get(blockId);
                String role = normalizeRole(part == null ? "" : part.role());
                if (!hasRole(role, "fuel")) {
                    return ValidationState.invalid("Fuel rod column has a gap or invalid segment");
                }
            }

            BlockPos capPos = new BlockPos(key.x(), maxY, key.z());
            String capId = BuiltInRegistries.BLOCK.getKey(level.getBlockState(capPos).getBlock()).toString();
            ReactorPartDefinition capPart = partsByBlockId.get(capId);
            String capRole = normalizeRole(capPart == null ? "" : capPart.role());
            if (!hasRole(capRole, "control")) {
                return ValidationState.invalid("Every fuel column must have a control rod cap");
            }
        }

        fuelColumns = fuelRodColumns.size();
        if (controlParts < fuelColumns) {
            return ValidationState.invalid("Not enough control rods for fuel columns");
        }

        return new ValidationState(
                true,
                "Assembled",
                sizeX,
                sizeY,
                sizeZ,
                structureParts,
                moderatorParts,
                controlParts,
                coolantParts,
                wasteParts,
                powerParts,
                accessParts,
                redstoneParts,
                fuelColumns,
                cooling,
                shielding,
                heatCapacity
        );
    }

    private static Map<String, ReactorPartDefinition> partsByBlockId() {
        Map<String, ReactorPartDefinition> byBlock = new HashMap<>();
        for (ReactorPartDefinition definition : activeParts()) {
            if (definition.blockId() == null || definition.blockId().isBlank()) {
                    continue;
                }
            byBlock.putIfAbsent(definition.blockId().trim().toLowerCase(Locale.ROOT), definition);
        }
        return byBlock;
    }

    private static List<ReactorPartDefinition> activeParts() {
        List<ReactorPartDefinition> loaded = new ArrayList<>(ReactorPartDataRegistry.registry().all());
        if (!loaded.isEmpty()) {
            return loaded;
        }

        return List.of(
                new ReactorPartDefinition("reactor_casing", "structure", MODID + ":reactor_casing", 160.0D, 0.0D, 3.2D),
                new ReactorPartDefinition("reactor_window", "window", MODID + ":reactor_window", 90.0D, 0.0D, 1.4D),
                new ReactorPartDefinition("reactor_fuel_rod", "fuel", MODID + ":reactor_fuel_rod", 50.0D, 0.0D, 0.0D),
                new ReactorPartDefinition("reactor_control_rod", "control", MODID + ":reactor_control_rod", 20.0D, 0.8D, 0.0D),
                new ReactorPartDefinition("reactor_access_port", "access", MODID + ":reactor_access_port", 80.0D, 0.0D, 1.0D),
                new ReactorPartDefinition("reactor_power_tap", "power", MODID + ":reactor_power_tap", 80.0D, 0.0D, 1.0D),
                new ReactorPartDefinition("reactor_coolant_port", "coolant", MODID + ":reactor_coolant_port", 80.0D, 4.0D, 1.0D),
                new ReactorPartDefinition("reactor_waste_port", "waste", MODID + ":reactor_waste_port", 80.0D, 0.0D, 1.0D),
                new ReactorPartDefinition("reactor_redstone_port", "redstone", MODID + ":reactor_redstone_port", 80.0D, 0.0D, 1.0D),
                new ReactorPartDefinition("reactor_controller", "controller", CONTROLLER_ID, 120.0D, 0.0D, 2.0D),
                new ReactorPartDefinition("reactor_moderator", "moderator", MODID + ":reactor_graphite_block", 80.0D, 0.4D, 0.5D)
        );
    }

    private static boolean isBoundary(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
    }

    private static boolean isBoundaryCandidateBlock(String blockId, Map<String, ReactorPartDefinition> partsByBlockId) {
        if (isControllerBlock(blockId)) {
            return true;
        }

        ReactorPartDefinition part = partsByBlockId.get(blockId);
        if (part == null) {
            return false;
        }

        String role = normalizeRole(part.role());
        return hasRole(role, "structure")
                || hasRole(role, "window")
                || hasRole(role, "controller")
                || hasRole(role, "control")
                || hasRole(role, "port")
                || hasRole(role, "power")
                || hasRole(role, "coolant")
                || hasRole(role, "waste")
                || hasRole(role, "access")
                || hasRole(role, "redstone");
    }

    private static boolean isControllerBlock(String blockId) {
        if (blockId == null) {
            return false;
        }

        String normalized = blockId.trim().toLowerCase(Locale.ROOT);
        return CONTROLLER_ID.equals(normalized) || CONTROLLER_COMPAT_ID.equals(normalized);
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        return role.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean hasRole(String role, String expected) {
        if (role.equals(expected)) {
            return true;
        }
        if (role.contains(",")) {
            String[] split = role.split(",");
            for (String candidate : split) {
                if (candidate.trim().equals(expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    public record ValidationState(
            boolean valid,
            String reason,
            int sizeX,
            int sizeY,
            int sizeZ,
            int structureParts,
            int moderatorParts,
            int controlParts,
            int coolantParts,
            int wasteParts,
            int powerParts,
            int accessParts,
            int redstoneParts,
            int fuelColumns,
            double coolingBonus,
            double shielding,
            double heatCapacity
    ) {
        public static ValidationState invalid(String reason) {
            return new ValidationState(false, reason, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0.0D, 0.0D, 0.0D);
        }
    }

    private record ColumnKey(int x, int z) {
    }

    private static final class ColumnSpan {
        private int minY;
        private int maxY;

        private ColumnSpan(int minY, int maxY) {
            this.minY = minY;
            this.maxY = maxY;
        }

        private void include(int y) {
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
    }

    private record ControllerKey(String dimension, BlockPos controllerPos) {
    }

    private record CacheEntry(ValidationState state, long expiresAtGameTick) {
    }
}
