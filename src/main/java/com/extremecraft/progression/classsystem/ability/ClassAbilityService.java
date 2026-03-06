package com.extremecraft.progression.classsystem.ability;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncClassAbilityStateS2CPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ClassDefinition;
import com.extremecraft.progression.classsystem.data.ClassAbilityDefinitions;
import com.extremecraft.progression.classsystem.data.ClassDefinitions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Server-authoritative class ability activation and cooldown management.
 */
public final class ClassAbilityService {
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new LinkedHashMap<>();

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
            cooldownsTag.putInt(entry.getKey(), remaining);
        }

        root.put("cooldowns", cooldownsTag);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncClassAbilityStateS2CPacket(root));
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
        long readyAt = cooldownsFor(player).getOrDefault(abilityId, 0L);
        return now >= readyAt;
    }

    private static boolean consumeManaCost(ServerPlayer player, int manaCost) {
        if (manaCost <= 0) {
            return true;
        }

        return PlayerStatsApi.get(player)
                .map(stats -> stats.tryConsumeMana(manaCost))
                .orElse(false);
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

        int strength = PlayerStatsApi.get(player).map(PlayerStatsCapability::strength).orElse(1);
        float damage = (float) (baseDamage + (strength * strengthMultiplier));

        List<LivingEntity> targets = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(3.0D),
                e -> e.isAlive() && e != player
        );

        if (targets.isEmpty()) {
            return false;
        }

        for (LivingEntity target : targets) {
            CombatEngine.applyDamage(DamageContext.builder()
                    .attacker(player)
                    .target(target)
                    .damageAmount(damage)
                    .damageType(DamageType.PHYSICAL)
                    .abilitySource("class:" + ability.id())
                    .weaponSource(player.getMainHandItem())
                    .armorValue(target.getArmorValue())
                    .build());
        }

        player.swing(player.getUsedItemHand(), true);
        return true;
    }

    private static Map<String, Long> cooldownsFor(ServerPlayer player) {
        return COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
    }
}
