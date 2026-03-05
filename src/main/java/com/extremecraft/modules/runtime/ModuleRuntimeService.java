package com.extremecraft.modules.runtime;

import com.extremecraft.modules.data.ModuleAbilityDefinition;
import com.extremecraft.modules.data.ModuleDefinition;
import com.extremecraft.modules.data.ModuleTrigger;
import com.extremecraft.modules.data.ModuleType;
import com.extremecraft.modules.item.IModularItem;
import com.extremecraft.modules.registry.ArmorModuleRegistry;
import com.extremecraft.modules.registry.ModuleAbilityRegistry;
import com.extremecraft.modules.registry.ToolModuleRegistry;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.SyncModuleAbilityStateS2CPacket;
import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModuleRuntimeService {
    private static final Map<UUID, Map<String, Long>> ABILITY_COOLDOWNS = new LinkedHashMap<>();
    private static final Map<UUID, Map<String, Float>> APPLIED_STAT_MODIFIERS = new LinkedHashMap<>();

    private ModuleRuntimeService() {
    }

    public static void applyPassiveModules(Player player, ItemStack sourceStack) {
        if (!(player instanceof ServerPlayer serverPlayer) || sourceStack.isEmpty()) {
            return;
        }

        if (!(sourceStack.getItem() instanceof IModularItem modularItem)) {
            return;
        }

        List<String> modules = modularItem.installedModules(sourceStack);
        if (modules.isEmpty()) {
            return;
        }

        PlayerStatsApi.get(serverPlayer).ifPresent(stats -> {
            Map<String, Float> active = collectStatModifiers(stats, modules, modularItem.moduleType());
            mergeEquipmentModifiers(serverPlayer, stats, active);
        });
    }

    public static boolean trigger(ServerPlayer player, ItemStack sourceStack, ModuleTrigger trigger, String contextId) {
        if (sourceStack.isEmpty() || !(sourceStack.getItem() instanceof IModularItem modularItem)) {
            return false;
        }

        boolean activated = false;
        for (String moduleId : modularItem.installedModules(sourceStack)) {
            ModuleDefinition module = module(modularItem.moduleType(), moduleId);
            if (module == null) {
                continue;
            }

            for (String abilityId : module.abilities()) {
                ModuleAbilityDefinition ability = ModuleAbilityRegistry.get(abilityId);
                if (ability == null || ability.trigger() != trigger) {
                    continue;
                }
                if (!isUnlocked(player, module) || !isOffCooldown(player, ability.id())) {
                    continue;
                }

                if (!PlayerStatsApi.get(player).map(stats -> stats.tryConsumeMana(ability.manaCost())).orElse(false)) {
                    continue;
                }

                if (applyAbility(player, ability, contextId)) {
                    setCooldown(player, ability.id(), ability.cooldownTicks());
                    PlayerStatsService.sync(player);
                    activated = true;
                }
            }
        }

        if (activated) {
            syncState(player);
        }
        return activated;
    }

    private static boolean applyAbility(ServerPlayer player, ModuleAbilityDefinition ability, String contextId) {
        switch (ability.id()) {
            case "kinetic_shield" -> {
                return true;
            }
            case "blink_step" -> {
                double distance = ability.scaling().getOrDefault("distance", 6.0D);
                var look = player.getLookAngle().normalize();
                double x = player.getX() + (look.x * distance);
                double z = player.getZ() + (look.z * distance);
                player.teleportTo(x, player.getY(), z);
                player.swing(InteractionHand.MAIN_HAND, true);
                return true;
            }
            case "mana_weave" -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    public static float shieldReduction(ServerPlayer player) {
        float reduction = 0.0F;

        for (ItemStack armor : player.getArmorSlots()) {
            if (!(armor.getItem() instanceof IModularItem modularItem)) {
                continue;
            }

            for (String moduleId : modularItem.installedModules(armor)) {
                ModuleDefinition module = module(ModuleType.ARMOR, moduleId);
                if (module == null || !isUnlocked(player, module)) {
                    continue;
                }

                for (String abilityId : module.abilities()) {
                    ModuleAbilityDefinition ability = ModuleAbilityRegistry.get(abilityId);
                    if (ability != null && "kinetic_shield".equals(ability.id()) && isOffCooldown(player, ability.id())) {
                        reduction = Math.max(reduction, ability.scaling().getOrDefault("damage_absorb", 0.20D).floatValue());
                    }
                }
            }
        }

        return Math.max(0.0F, Math.min(0.85F, reduction));
    }

    public static void consumeShieldCooldown(ServerPlayer player) {
        for (ItemStack armor : player.getArmorSlots()) {
            if (!(armor.getItem() instanceof IModularItem modularItem)) {
                continue;
            }

            for (String moduleId : modularItem.installedModules(armor)) {
                ModuleDefinition module = module(ModuleType.ARMOR, moduleId);
                if (module == null || !isUnlocked(player, module)) {
                    continue;
                }
                for (String abilityId : module.abilities()) {
                    ModuleAbilityDefinition ability = ModuleAbilityRegistry.get(abilityId);
                    if (ability != null && "kinetic_shield".equals(ability.id()) && isOffCooldown(player, ability.id())) {
                        setCooldown(player, ability.id(), ability.cooldownTicks());
                        syncState(player);
                        return;
                    }
                }
            }
        }
    }

    public static void syncState(ServerPlayer player) {
        CompoundTag root = new CompoundTag();
        CompoundTag cooldownTag = new CompoundTag();

        long now = player.level().getGameTime();
        for (Map.Entry<String, Long> entry : cooldownsFor(player).entrySet()) {
            int remaining = (int) Math.max(0, entry.getValue() - now);
            cooldownTag.putInt(entry.getKey(), remaining);
        }

        root.put("cooldowns", cooldownTag);
        ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncModuleAbilityStateS2CPacket(root));
    }

    private static Map<String, Float> collectStatModifiers(PlayerStatsCapability stats, List<String> moduleIds, ModuleType type) {
        Map<String, Float> active = new LinkedHashMap<>();

        for (String moduleId : moduleIds) {
            ModuleDefinition module = module(type, moduleId);
            if (module == null || !isUnlocked(stats, module)) {
                continue;
            }

            for (Map.Entry<String, Float> stat : module.statModifiers().entrySet()) {
                active.merge(stat.getKey(), stat.getValue(), Float::sum);
            }
        }

        return active;
    }

    private static void mergeEquipmentModifiers(ServerPlayer player, PlayerStatsCapability stats, Map<String, Float> next) {
        Map<String, Float> previous = APPLIED_STAT_MODIFIERS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
        boolean changed = false;

        for (String prevKey : java.util.Set.copyOf(previous.keySet())) {
            if (!next.containsKey(prevKey)) {
                stats.removeEquipmentModifier(prevKey);
                previous.remove(prevKey);
                changed = true;
            }
        }

        for (Map.Entry<String, Float> entry : next.entrySet()) {
            Float prev = previous.get(entry.getKey());
            if (prev == null || Math.abs(prev - entry.getValue()) > 0.0001F) {
                stats.setEquipmentModifier(entry.getKey(), entry.getValue());
                previous.put(entry.getKey(), entry.getValue());
                changed = true;
            }
        }

        if (changed) {
            stats.recalculateDerivedStats();
            PlayerStatsService.sync(player, stats);
        }
    }

    private static ModuleDefinition module(ModuleType type, String id) {
        return type == ModuleType.ARMOR ? ArmorModuleRegistry.get(id) : ToolModuleRegistry.get(id);
    }

    private static boolean isUnlocked(ServerPlayer player, ModuleDefinition module) {
        return PlayerStatsApi.get(player).map(stats -> isUnlocked(stats, module)).orElse(false);
    }

    private static boolean isUnlocked(PlayerStatsCapability stats, ModuleDefinition module) {
        return module.requiredSkillNode().isBlank() || stats.isSkillUnlocked(module.requiredSkillNode());
    }

    private static boolean isOffCooldown(ServerPlayer player, String abilityId) {
        return player.level().getGameTime() >= cooldownsFor(player).getOrDefault(abilityId, 0L);
    }

    private static void setCooldown(ServerPlayer player, String abilityId, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return;
        }
        cooldownsFor(player).put(abilityId, player.level().getGameTime() + cooldownTicks);
    }

    private static Map<String, Long> cooldownsFor(ServerPlayer player) {
        return ABILITY_COOLDOWNS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>());
    }
}
