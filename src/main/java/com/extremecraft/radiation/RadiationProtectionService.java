package com.extremecraft.radiation;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

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

    public static boolean isHazmatSuitEquipped(Player player) {
        if (player == null) {
            return false;
        }

        int equippedArmorPieces = 0;
        int hazmatPieces = 0;
        int leadPieces = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.isEmpty()) {
                continue;
            }

            equippedArmorPieces++;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().toLowerCase(Locale.ROOT);
            if (id.contains("hazmat") || id.contains("radiation_suit")) {
                hazmatPieces++;
            }
            if (id.contains("lead")) {
                leadPieces++;
            }
        }

        if (equippedArmorPieces < 4) {
            return false;
        }
        return hazmatPieces >= 3 || leadPieces >= 4;
    }
}
