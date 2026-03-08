package com.extremecraft.reactor;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.platform.data.definition.ReactorPartDefinition;
import com.extremecraft.platform.data.registry.ReactorPartDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReactorMultiblockService {
    private static final Map<ControllerKey, ValidationState> CACHE = new ConcurrentHashMap<>();
    private static final int SCAN_RADIUS = 2;

    private ReactorMultiblockService() {
    }

    public static ValidationState validate(ServerLevel level, BlockPos controllerPos) {
        ControllerKey key = new ControllerKey(level.dimension().location().toString(), controllerPos.immutable());
        return CACHE.computeIfAbsent(key, ignored -> computeValidation(level, controllerPos));
    }

    public static void invalidateAround(Level level, BlockPos changedPos) {
        if (level == null || changedPos == null) {
            return;
        }

        int range = Math.max(SCAN_RADIUS + 1, ECFoundationConfig.structureInvalidationRange());
        int maxDist = range * range;
        String dimension = level.dimension().location().toString();
        CACHE.keySet().removeIf(key -> key.dimension.equals(dimension) && key.controllerPos.distSqr(changedPos) <= maxDist);
    }

    private static ValidationState computeValidation(ServerLevel level, BlockPos controllerPos) {
        int structureParts = 0;
        int moderatorParts = 0;
        int controlParts = 0;
        int coolantParts = 0;
        int wasteParts = 0;
        double cooling = 0.0D;
        double shielding = 0.0D;
        double heatCapacity = 0.0D;

        List<ReactorPartDefinition> parts = activeParts();
        for (BlockPos scanPos : BlockPos.betweenClosed(controllerPos.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS), controllerPos.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {
            if (scanPos.equals(controllerPos)) {
                continue;
            }

            BlockState state = level.getBlockState(scanPos);
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            for (ReactorPartDefinition part : parts) {
                if (!part.blockId().equals(blockId)) {
                    continue;
                }

                heatCapacity += part.heatCapacity();
                cooling += part.cooling();
                shielding += part.shielding();
                switch (part.role()) {
                    case "structure", "casing", "shielding" -> structureParts++;
                    case "moderator", "fuel" -> moderatorParts++;
                    case "control" -> controlParts++;
                    case "coolant" -> coolantParts++;
                    case "waste", "port" -> wasteParts++;
                    default -> {
                    }
                }
            }
        }

        boolean valid = structureParts >= 4 && moderatorParts >= 1 && controlParts >= 1 && coolantParts >= 1 && wasteParts >= 1;
        return new ValidationState(valid, structureParts, moderatorParts, controlParts, coolantParts, wasteParts, cooling, shielding, heatCapacity);
    }

    private static List<ReactorPartDefinition> activeParts() {
        List<ReactorPartDefinition> loaded = new ArrayList<>(ReactorPartDataRegistry.registry().all());
        if (!loaded.isEmpty()) {
            return loaded;
        }

        return List.of(
                new ReactorPartDefinition("fission_casing", "structure", "extremecraft:lead_block", 120.0D, 0.0D, 1.5D),
                new ReactorPartDefinition("moderator_stack", "moderator", "extremecraft:uranium_block", 60.0D, 0.0D, 0.4D),
                new ReactorPartDefinition("control_rod", "control", "minecraft:lightning_rod", 20.0D, 0.3D, 0.0D),
                new ReactorPartDefinition("coolant_injector", "coolant", "minecraft:water", 0.0D, 3.8D, 0.0D),
                new ReactorPartDefinition("waste_outlet", "waste", "minecraft:hopper", 10.0D, 0.0D, 0.0D)
        );
    }

    public record ValidationState(
            boolean valid,
            int structureParts,
            int moderatorParts,
            int controlParts,
            int coolantParts,
            int wasteParts,
            double coolingBonus,
            double shielding,
            double heatCapacity
    ) {
    }

    private record ControllerKey(String dimension, BlockPos controllerPos) {
    }
}
