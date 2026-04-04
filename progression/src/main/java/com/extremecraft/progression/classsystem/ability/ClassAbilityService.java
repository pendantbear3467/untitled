package com.extremecraft.progression.classsystem.ability;

import com.extremecraft.ability.AbilityContext;
import com.extremecraft.ability.AbilityDefinition;
import com.extremecraft.ability.AbilityEffect;
import com.extremecraft.ability.AbilityExecutor;
import com.extremecraft.ability.AbilityTargetResolver;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncClassAbilityStateS2CPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ClassDefinition;
import com.extremecraft.progression.classsystem.data.ClassAbilityDefinitions;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;
import com.extremecraft.progression.unlock.UnlockAccessService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative class ability activation and cooldown management.
 *
 * <p>Class ability triggers remain distinct from generic abilities and spells, but compiled class
 * ability payloads now execute through {@code AbilityExecutor.executeDefinition} so effect
 * ownership is not split across parallel runtime pipelines.</p>
 */
public final class ClassAbilityService {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new ConcurrentHashMap<>();

    private ClassAbilityService() {
    }

    public static boolean tryActivate(ServerPlayer player, String requestedAbilityId) {
        if (player == null) {
            return false;
        }

        String abilityId = resolveAbilityId(player, requestedAbilityId);
        if (abilityId.isBlank()) {
            return false;
        }

        ClassAbilityDefinition ability = ClassAbilityDefinitions.get(abilityId);
        if (ability == null) {
            return false;
        }

        if (!UnlockAccessService.canUseClassAbility(player, ability.id())) {
            syncState(player);
            return false;
        }

        String currentClass = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        if (!ability.classId().isBlank() && !ability.classId().equalsIgnoreCase(currentClass)) {
            return false;
        }

        long now = player.level().getGameTime();
        if (!isOffCooldown(player, ability.id(), now)) {
            syncState(player);
            return false;
        }

        if (!consumeManaCost(player, ability.manaCost())) {
            syncState(player);
            return false;
        }

        boolean applied = applyAbilityEffect(player, ability);
        if (!applied) {
            syncState(player);
            return false;
        }

        if (ability.cooldownTicks() > 0) {
            cooldownsFor(player).put(ability.id(), now + ability.cooldownTicks());
        }

        syncState(player);
        return true;
    }

    public static void syncState(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        CompoundTag cooldownsTag = new CompoundTag();

        long now = player.level().getGameTime();
        for (Map.Entry<String, Long> entry : cooldownsFor(player).entrySet()) {
            int remaining = (int) Math.max(0, entry.getValue() - now);
            if (remaining > 0) {
                cooldownsTag.putInt(entry.getKey(), remaining);
            }
        }

        root.put("cooldowns", cooldownsTag);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncClassAbilityStateS2CPacket(root));
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            clearPlayer(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            COOLDOWNS.remove(playerId);
        }
    }

    private static String resolveAbilityId(ServerPlayer player, String requestedAbilityId) {
        if (requestedAbilityId != null && !requestedAbilityId.isBlank()) {
            return requestedAbilityId.trim().toLowerCase();
        }

        String currentClass = ProgressApi.get(player).map(data -> data.currentClass()).orElse("warrior");
        ClassDefinition classDefinition = ClassDefinitions.get(currentClass);
        if (classDefinition == null || classDefinition.activeAbilities().isEmpty()) {
            return "";
        }

        return classDefinition.activeAbilities().get(0);
    }

    private static boolean isOffCooldown(ServerPlayer player, String abilityId, long now) {
        Map<String, Long> perPlayer = cooldownsFor(player);
        long readyAt = perPlayer.getOrDefault(abilityId, 0L);
        if (readyAt <= now) {
            perPlayer.remove(abilityId);
            return true;
        }
        return false;
    }

    private static boolean consumeManaCost(ServerPlayer player, int manaCost) {
        if (manaCost <= 0) {
            return true;
        }

        return ManaService.tryConsume(player, manaCost);
    }

    private static boolean applyAbilityEffect(ServerPlayer player, ClassAbilityDefinition ability) {
        if ("warrior_cleave".equals(ability.id())) {
            return applyWarriorCleave(player, ability);
        }

        return false;
    }

    private static boolean applyWarriorCleave(ServerPlayer player, ClassAbilityDefinition ability) {
        double baseDamage = ability.scaling().getOrDefault("base_damage", 6.0D);
        double strengthMultiplier = ability.scaling().getOrDefault("strength_multiplier", 1.25D);
        double radius = Math.max(1.0D, ability.scaling().getOrDefault("radius", 3.0D));

        int strength = PlayerStatsApi.get(player).map(PlayerStatsCapability::strength).orElse(1);
        AbilityDefinition compiled = new AbilityDefinition(
                "class:" + ability.id(),
                ability.manaCost(),
                ability.cooldownTicks(),
                AbilityDefinition.TargetType.AREA,
                radius,
                radius,
                ability.classId(),
                List.of(new AbilityEffect(
                        "damage",
                        baseDamage + (strength * strengthMultiplier),
                        0,
                        0,
                        "physical",
                        Map.of()
                ))
        );

        AbilityContext context = AbilityContext.of(player, compiled);
        if (AbilityTargetResolver.resolve(context).entities().isEmpty()) {
            return false;
        }

        if (!AbilityExecutor.executeDefinition(context)) {
            return false;
        }

        player.swing(player.getUsedItemHand(), true);
        return true;
    }

    private static Map<String, Long> cooldownsFor(ServerPlayer player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new ConcurrentHashMap<>());
    }
}
