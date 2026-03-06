package com.extremecraft.progression;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class BuffStackingSystem {
    public record BuffState(String id, int stacks, int amplifier, long expiresAtTick) {
    }

    private static final Map<UUID, Map<String, BuffState>> ACTIVE = new LinkedHashMap<>();

    private BuffStackingSystem() {
    }

    public static void track(LivingEntity entity, String buffId, int durationTicks, int amplifier) {
        if (!(entity.level() instanceof ServerLevel serverLevel) || buffId == null || buffId.isBlank()) {
            return;
        }

        long expiresAt = serverLevel.getGameTime() + Math.max(1, durationTicks);
        Map<String, BuffState> buffs = ACTIVE.computeIfAbsent(entity.getUUID(), key -> new LinkedHashMap<>());

        BuffState previous = buffs.get(buffId);
        int stacks = previous == null ? 1 : Math.min(16, previous.stacks() + 1);
        int mergedAmplifier = Math.max(amplifier, previous == null ? 0 : previous.amplifier());
        buffs.put(buffId, new BuffState(buffId, stacks, mergedAmplifier, expiresAt));
    }

    public static Map<String, BuffState> activeFor(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return Map.of();
        }

        Map<String, BuffState> buffs = ACTIVE.get(entity.getUUID());
        if (buffs == null || buffs.isEmpty()) {
            return Map.of();
        }

        long now = serverLevel.getGameTime();
        buffs.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= now);
        if (buffs.isEmpty()) {
            ACTIVE.remove(entity.getUUID());
            return Map.of();
        }

        return Map.copyOf(buffs);
    }
}
