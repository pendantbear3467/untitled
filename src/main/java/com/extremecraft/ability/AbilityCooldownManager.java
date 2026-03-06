package com.extremecraft.ability;

import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.classsystem.PlayerClass;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class AbilityCooldownManager {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new LinkedHashMap<>();

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
        long readyAt = cooldowns(player).getOrDefault(normalize(abilityId), 0L);
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
        for (Map.Entry<String, Long> entry : cooldowns(player).entrySet()) {
            int remaining = (int) Math.max(0L, entry.getValue() - now);
            if (remaining > 0) {
                cooldownTag.putInt(entry.getKey(), remaining);
            }
        }

        String classId = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        PlayerClass playerClass = ClassRegistry.get(classId);
        List<String> abilities = playerClass == null ? List.of() : playerClass.abilityAccess();

        for (int slot = 1; slot <= 4; slot++) {
            String key = "slot_" + slot;
            String abilityId = slot <= abilities.size() ? abilities.get(slot - 1) : "";
            slotTag.putString(key, abilityId == null ? "" : abilityId);
            int manaCost = AbilityRegistry.get(abilityId) == null ? 0 : AbilityRegistry.get(abilityId).manaCost();
            slotManaTag.putInt(key, manaCost);
        }

        root.put("cooldowns", cooldownTag);
        root.put("slots", slotTag);
        root.put("slot_mana", slotManaTag);
        return root;
    }

    private static Map<String, Long> cooldowns(ServerPlayer player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
    }

    private static String normalize(String abilityId) {
        return abilityId == null ? "" : abilityId.trim().toLowerCase();
    }
}
