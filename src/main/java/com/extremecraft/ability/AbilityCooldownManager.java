package com.extremecraft.ability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class AbilityCooldownManager {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new LinkedHashMap<>();

    private AbilityCooldownManager() {
    }

    public static boolean isReady(ServerPlayer player, String abilityId) {
        return remainingTicks(player, abilityId) <= 0;
    }

    public static int remainingTicks(ServerPlayer player, String abilityId) {
        long now = player.level().getGameTime();
        long readyAt = cooldowns(player).getOrDefault(abilityId, 0L);
        return (int) Math.max(0L, readyAt - now);
    }

    public static void startCooldown(ServerPlayer player, String abilityId, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }

        cooldowns(player).put(abilityId, player.level().getGameTime() + cooldownTicks);
    }

    public static CompoundTag serializeFor(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();

        long now = player.level().getGameTime();
        for (Map.Entry<String, Long> entry : cooldowns(player).entrySet()) {
            int remaining = (int) Math.max(0L, entry.getValue() - now);
            if (remaining > 0) {
                cooldownTag.putInt(entry.getKey(), remaining);
            }
        }

        root.put("cooldowns", cooldownTag);
        return root;
    }

    private static Map<String, Long> cooldowns(ServerPlayer player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
    }
}
