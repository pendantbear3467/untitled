package com.extremecraft.modules.service;

import com.extremecraft.modules.data.ModuleDefinition;
import com.extremecraft.modules.data.ModuleType;
import com.extremecraft.modules.item.IModularItem;
import com.extremecraft.modules.item.ModuleStackData;
import com.extremecraft.modules.registry.ArmorModuleRegistry;
import com.extremecraft.modules.registry.ToolModuleRegistry;
import com.extremecraft.modules.runtime.ModuleRuntimeService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModuleInstallService {
    public enum TargetSlot {
        MAIN_HAND,
        OFF_HAND,
        CHESTPLATE;

        public static TargetSlot byName(String value) {
            if (value == null || value.isBlank()) {
                return MAIN_HAND;
            }
            for (TargetSlot slot : values()) {
                if (slot.name().equalsIgnoreCase(value.trim())) {
                    return slot;
                }
            }
            return MAIN_HAND;
        }
    }

    public enum Result {
        SUCCESS,
        NOT_MODULAR_ITEM,
        MODULE_NOT_FOUND,
        DUPLICATE,
        NO_SLOTS,
        LOCKED_BY_SKILL,
        INVALID_INPUT
    }

    private ModuleInstallService() {
    }

    public static Result install(ServerPlayer player, TargetSlot targetSlot, String moduleId) {
        if (player == null || moduleId == null || moduleId.isBlank()) {
            return Result.INVALID_INPUT;
        }

        ItemStack stack = stackFor(player, targetSlot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IModularItem modularItem)) {
            return Result.NOT_MODULAR_ITEM;
        }

        ModuleDefinition module = module(modularItem.moduleType(), moduleId);
        if (module == null) {
            return Result.MODULE_NOT_FOUND;
        }

        if (!module.requiredSkillNode().isBlank()) {
            boolean unlocked = com.extremecraft.progression.capability.PlayerStatsApi.get(player)
                    .map(stats -> stats.isSkillUnlocked(module.requiredSkillNode()))
                    .orElse(false);
            if (!unlocked) {
                return Result.LOCKED_BY_SKILL;
            }
        }

        List<String> installed = new ArrayList<>(modularItem.installedModules(stack));
        if (installed.contains(module.id())) {
            return Result.DUPLICATE;
        }

        int used = slotsUsed(installed, modularItem.moduleType());
        if ((used + module.slotCost()) > modularItem.moduleSlots(stack)) {
            return Result.NO_SLOTS;
        }

        installed.add(module.id());
        ModuleStackData.writeModules(stack, new LinkedHashSet<>(installed));
        onModuleChanged(player);
        return Result.SUCCESS;
    }

    public static Result remove(ServerPlayer player, TargetSlot targetSlot, String moduleId) {
        if (player == null || moduleId == null || moduleId.isBlank()) {
            return Result.INVALID_INPUT;
        }

        ItemStack stack = stackFor(player, targetSlot);
        if (stack.isEmpty() || !(stack.getItem() instanceof IModularItem modularItem)) {
            return Result.NOT_MODULAR_ITEM;
        }

        Set<String> installed = new LinkedHashSet<>(modularItem.installedModules(stack));
        if (!installed.remove(moduleId.trim().toLowerCase())) {
            return Result.INVALID_INPUT;
        }

        ModuleStackData.writeModules(stack, installed);
        onModuleChanged(player);
        return Result.SUCCESS;
    }

    private static int slotsUsed(List<String> modules, ModuleType type) {
        int used = 0;
        for (String id : modules) {
            ModuleDefinition module = module(type, id);
            if (module != null) {
                used += Math.max(1, module.slotCost());
            }
        }
        return used;
    }

    private static ItemStack stackFor(ServerPlayer player, TargetSlot slot) {
        return switch (slot) {
            case MAIN_HAND -> player.getItemInHand(InteractionHand.MAIN_HAND);
            case OFF_HAND -> player.getItemInHand(InteractionHand.OFF_HAND);
            case CHESTPLATE -> player.getItemBySlot(EquipmentSlot.CHEST);
        };
    }

    private static ModuleDefinition module(ModuleType type, String id) {
        String key = id.trim().toLowerCase();
        return type == ModuleType.ARMOR ? ArmorModuleRegistry.get(key) : ToolModuleRegistry.get(key);
    }

    private static void onModuleChanged(ServerPlayer player) {
        ModuleRuntimeService.refreshPassiveModifiers(player);
        ModuleRuntimeService.syncState(player);
    }
}
