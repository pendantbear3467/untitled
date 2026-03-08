package com.extremecraft.entity.system;

import com.extremecraft.combat.CombatEngine;
import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import com.extremecraft.config.Config;
import com.extremecraft.machine.core.TechMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GameplayMechanicsEvents {
    private static final long HAZARD_CACHE_TTL_TICKS = 20L;
    private static final long HAZARD_CACHE_PRUNE_INTERVAL_TICKS = 200L;
    private static final int HAZARD_CACHE_MAX_ENTRIES = 4096;
    private static final Map<String, HazardSnapshot> HAZARD_SCAN_CACHE = new ConcurrentHashMap<>();

    private static volatile long lastPruneTick;

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!Config.isMachineHazardEnabled()) {
            return;
        }

        int interval = Config.machineHazardScanIntervalTicks();
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) != 0) {
            return;
        }

        int horizontalRadius = Config.machineHazardHorizontalRadius();
        int verticalRadius = Config.machineHazardVerticalRadius();

        BlockPos center = player.blockPosition();
        ServerLevel level = player.serverLevel();
        int activeMachines = countActiveMachines(level, center, horizontalRadius, verticalRadius);

        int requiredMachines = Config.machineHazardRequiredMachines();
        if (activeMachines >= requiredMachines) {
            if (!hasProtection(player)) {
                float damage = (float) Config.machineHazardDamage();
                if (damage > 0.0F) {
                    CombatEngine.applyDamage(DamageContext.builder()
                            .attacker(null)
                            .target(player)
                            .damageAmount(damage)
                            .damageType(DamageType.MAGIC)
                            .abilitySource("environment:machine_hazard")
                            .weaponSource(ItemStack.EMPTY)
                            .armorValue(player.getArmorValue())
                            .build());
                }
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
            }
        }
    }

    private static int countActiveMachines(ServerLevel level, BlockPos center, int horizontalRadius, int verticalRadius) {
        long now = level.getGameTime();
        String cacheKey = level.dimension().location() + "|" + (center.getX() >> 4) + "|" + (center.getZ() >> 4)
                + "|" + horizontalRadius + "|" + verticalRadius;

        HazardSnapshot cached = HAZARD_SCAN_CACHE.get(cacheKey);
        if (cached != null && (now - cached.tick()) <= HAZARD_CACHE_TTL_TICKS) {
            return cached.activeMachines();
        }

        int activeMachines = 0;
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius),
                center.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TechMachineBlockEntity machine
                    && machine.getEnergyStorageExt().getEnergyStored() > machine.getEnergyStorageExt().getMaxEnergyStored() * 0.8F) {
                activeMachines++;
            }
        }

        pruneHazardCache(now);
        HAZARD_SCAN_CACHE.put(cacheKey, new HazardSnapshot(now, activeMachines));
        return activeMachines;
    }

    private static void pruneHazardCache(long now) {
        if ((now - lastPruneTick) < HAZARD_CACHE_PRUNE_INTERVAL_TICKS && HAZARD_SCAN_CACHE.size() < HAZARD_CACHE_MAX_ENTRIES) {
            return;
        }

        lastPruneTick = now;
        HAZARD_SCAN_CACHE.entrySet().removeIf(entry -> (now - entry.getValue().tick()) > HAZARD_CACHE_TTL_TICKS);

        if (HAZARD_SCAN_CACHE.size() <= HAZARD_CACHE_MAX_ENTRIES) {
            return;
        }

        Iterator<String> keys = HAZARD_SCAN_CACHE.keySet().iterator();
        while (HAZARD_SCAN_CACHE.size() > HAZARD_CACHE_MAX_ENTRIES && keys.hasNext()) {
            keys.next();
            keys.remove();
        }
    }

    @SubscribeEvent
    public void onCombatCombo(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean combo = player.hasEffect(MobEffects.DAMAGE_BOOST) && player.hasEffect(MobEffects.MOVEMENT_SPEED);
        if (combo) {
            event.setAmount(event.getAmount() * 1.15F);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));
        }
    }

    private boolean hasProtection(ServerPlayer player) {
        ItemStack chest = player.getInventory().armor.get(2);
        return !chest.isEmpty() && chest.getDescriptionId().contains("pioneer_chestplate");
    }

    private record HazardSnapshot(long tick, int activeMachines) {
    }
}
