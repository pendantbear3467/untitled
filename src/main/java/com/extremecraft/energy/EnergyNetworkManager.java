package com.extremecraft.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class EnergyNetworkManager {
    private static final Map<ResourceKey<Level>, Set<BlockPos>> PRODUCERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CONSUMERS = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> CABLES = new HashMap<>();

    private EnergyNetworkManager() {}

    public static void registerProducer(Level level, BlockPos pos) {
        PRODUCERS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void registerConsumer(Level level, BlockPos pos) {
        CONSUMERS.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void registerCable(Level level, BlockPos pos) {
        CABLES.computeIfAbsent(level.dimension(), key -> new HashSet<>()).add(pos.immutable());
    }

    public static void unregister(Level level, BlockPos pos) {
        Set<BlockPos> producers = PRODUCERS.get(level.dimension());
        if (producers != null) {
            producers.remove(pos);
        }

        Set<BlockPos> consumers = CONSUMERS.get(level.dimension());
        if (consumers != null) {
            consumers.remove(pos);
        }

        Set<BlockPos> cables = CABLES.get(level.dimension());
        if (cables != null) {
            cables.remove(pos);
        }
    }

    public static int cableCount(Level level) {
        return CABLES.getOrDefault(level.dimension(), Set.of()).size();
    }

    public static void tick(Level level) {
        // Cable transfer is currently handled by CableBlockEntity ticks.
    }
}
