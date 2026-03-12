package com.extremecraft.radiation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class RadiationProtectionService {
    private RadiationProtectionService() {
    }

    public static double protectionFactor(Player player) {
        if (player == null) {
            return 0.0D;
        }

        double protection = 0.0D;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.contains("lead")) {
                protection += 0.18D;
            } else if (id.contains("celestial") || id.contains("aether")) {
                protection += 0.12D;
            } else if (id.contains("netherite")) {
                protection += 0.06D;
            }
        }
        return Math.min(0.85D, protection);
    }

    public static double cleanupEfficiency(Player player) {
        double protection = protectionFactor(player);
        if (protection <= 0.0D) {
            return 0.0D;
        }

        double efficiency = protection;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) {
                continue;
            }
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.contains("lead")) {
                efficiency += 0.08D;
            }
        }
        return Math.min(1.0D, efficiency);
    }
}
