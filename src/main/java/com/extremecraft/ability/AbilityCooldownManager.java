package com.extremecraft.ability;

import com.extremecraft.classsystem.ClassAccessResolver;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AbilityCooldownManager {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    private AbilityCooldownManager() {
    }

    public static boolean isReady(ServerPlayer player, String abilityId) {
        return remainingTicks(player, abilityId) <= 0;
    }

    public static boolean isActive(ServerPlayer player, String abilityId) {
        return remainingTicks(player, abilityId) > 0;
    }

    public static int remainingTicks(ServerPlayer player, String abilityId) {
        if (player == null) {
            return 0;
        }

        long now = player.level().getGameTime();
        Map<String, Long> perPlayer = cooldowns(player);
        String key = normalize(abilityId);
        long readyAt = perPlayer.getOrDefault(key, 0L);
        if (readyAt <= now) {
            perPlayer.remove(key);
            return 0;
        }

        return (int) Math.max(0L, readyAt - now);
    }

    public static void startCooldown(ServerPlayer player, String abilityId, int cooldownTicks) {
        if (player == null || cooldownTicks <= 0) {
            return;
        }

        String key = normalize(abilityId);
        if (key.isBlank()) {
            return;
        }

        cooldowns(player).put(key, player.level().getGameTime() + cooldownTicks);
    }

    public static void sync(ServerPlayer player) {
        if (player != null) {
            RuntimeSyncService.syncAbilities(player);
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            COOLDOWNS.remove(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            COOLDOWNS.remove(playerId);
        }
    }

    public static CompoundTag serializeFor(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();
        CompoundTag slotTag = new CompoundTag();
        CompoundTag slotManaTag = new CompoundTag();

        if (player == null) {
            root.put("cooldowns", cooldownTag);
            root.put("slots", slotTag);
            root.put("slot_mana", slotManaTag);
            return root;
        }

        long now = player.level().getGameTime();
        Map<String, Long> perPlayerCooldowns = cooldowns(player);
        for (Map.Entry<String, Long> entry : perPlayerCooldowns.entrySet()) {
            int remaining = (int) Math.max(0L, entry.getValue() - now);
            if (remaining > 0) {
                cooldownTag.putInt(entry.getKey(), remaining);
            }
        }

        String classId = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        List<String> abilities = ClassAccessResolver.abilityAccess(classId);

        for (int slot = 1; slot <= 4; slot++) {
            String key = "slot_" + slot;
            String abilityId = slot <= abilities.size() ? abilities.get(slot - 1) : "";
            String normalizedAbilityId = normalize(abilityId);
            slotTag.putString(key, normalizedAbilityId);
            int manaCost = AbilityRegistry.get(normalizedAbilityId) == null ? 0 : AbilityRegistry.get(normalizedAbilityId).manaCost();
            slotManaTag.putInt(key, manaCost);
        }

        root.put("cooldowns", cooldownTag);
        root.put("slots", slotTag);
        root.put("slot_mana", slotManaTag);
        return root;
    }

    private static Map<String, Long> cooldowns(ServerPlayer player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
    }

    private static String normalize(String abilityId) {
        return abilityId == null ? "" : abilityId.trim().toLowerCase();
    }
}
