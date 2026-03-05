package com.extremecraft.item.module;

import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Applies equipped tool/armor module effects into PlayerStatsCapability.
 */
public final class ModuleEffectService {
    private ModuleEffectService() {
    }

    public static boolean applyEquippedModules(ServerPlayer player, PlayerStatsCapability stats) {
        Map<String, Float> effects = new LinkedHashMap<>();

        applyArmorStack(player.getItemBySlot(EquipmentSlot.HEAD), effects);
        applyArmorStack(player.getItemBySlot(EquipmentSlot.CHEST), effects);
        applyArmorStack(player.getItemBySlot(EquipmentSlot.LEGS), effects);
        applyArmorStack(player.getItemBySlot(EquipmentSlot.FEET), effects);

        applyToolStack(player.getMainHandItem(), effects);
        applyToolStack(player.getOffhandItem(), effects);

        return stats.replaceEquipmentModifiers(effects);
    }

    private static void applyArmorStack(ItemStack stack, Map<String, Float> effects) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        for (ItemModuleStorage.InstalledModule installed : ItemModuleStorage.getModules(stack)) {
            ModuleDefinition definition = ModuleRegistry.get(installed.id());
            if (definition == null) {
                continue;
            }
            definition.applyArmorEffects(stack, installed.level(), effects);
        }
    }

    private static void applyToolStack(ItemStack stack, Map<String, Float> effects) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        for (ItemModuleStorage.InstalledModule installed : ItemModuleStorage.getModules(stack)) {
            ModuleDefinition definition = ModuleRegistry.get(installed.id());
            if (definition == null) {
                continue;
            }
            definition.applyToolEffects(stack, installed.level(), effects);
        }
    }
}
