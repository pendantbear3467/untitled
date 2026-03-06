package com.extremecraft.progression;

import com.extremecraft.classsystem.ClassAbilityBindings;
import com.extremecraft.classsystem.ClassPassives;
import com.extremecraft.classsystem.PlayerClass;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class StatCalculationEngine {
    public record PlayerStatSnapshot(Map<String, Double> values) {
        public double get(String id) {
            return values.getOrDefault(id, 0.0D);
        }
    }

    private StatCalculationEngine() {
    }

    public static PlayerStatSnapshot calculate(ServerPlayer player) {
        PlayerStatsCapability stats = PlayerStatsApi.get(player).orElse(null);
        if (stats == null) {
            return new PlayerStatSnapshot(Map.of());
        }

        Map<String, Double> values = new LinkedHashMap<>();
        values.put("strength", (double) stats.strength());
        values.put("agility", (double) stats.agility());
        values.put("intelligence", (double) stats.intelligence());
        values.put("vitality", (double) stats.vitality());
        values.put("luck", (double) stats.luck());
        values.put("spell_power", (double) stats.magicPower());
        values.put("attack_speed", (double) stats.attackSpeed());

        AttributeModifiers modifiers = new AttributeModifiers();

        PlayerClass playerClass = ClassAbilityBindings.current(player);
        if (playerClass != null) {
            Map<String, Double> passives = ClassPassives.resolve(playerClass);
            passives.forEach(modifiers::add);
            playerClass.statScaling().forEach((stat, scale) -> modifiers.multiply(stat, scale - 1.0D));
        }

        BuffStackingSystem.activeFor(player).values().forEach(buff -> {
            if (buff.id().startsWith("buff:spell_power")) {
                modifiers.add("spell_power", buff.stacks() * 0.5D);
            }
            if (buff.id().startsWith("buff:attack_speed")) {
                modifiers.add("attack_speed", buff.stacks() * 0.025D);
            }
        });

        Map<String, Double> calculated = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : values.entrySet()) {
            calculated.put(entry.getKey(), modifiers.apply(entry.getKey(), entry.getValue()));
        }

        return new PlayerStatSnapshot(Map.copyOf(calculated));
    }
}
