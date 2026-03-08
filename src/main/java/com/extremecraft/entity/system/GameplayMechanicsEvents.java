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
import java.util.concurrent.atomic.LongAdder;

public final class GameplayMechanicsEvents {
    private static final long HAZARD_CACHE_TTL_TICKS = 20L;
    private static final long HAZARD_CACHE_PRUNE_INTERVAL_TICKS = 200L;
    private static final int HAZARD_CACHE_MAX_ENTRIES = 4096;
    private static final Map<String, HazardSnapshot> HAZARD_SCAN_CACHE = new ConcurrentHashMap<>();`r`n    private static final LongAdder HAZARD_CACHE_HITS = new LongAdder();`r`n    private static final LongAdder HAZARD_CACHE_MISSES = new LongAdder();`r`n    private static final LongAdder HAZARD_CACHE_PRUNED_ENTRIES = new LongAdder();`r`n    private static final LongAdder HAZARD_CACHE_FORCED_EVICTIONS = new LongAdder();`r`n`r`n    private static volatile long lastPruneTick;

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

        HazardSnapshot cached = HAZARD_SCAN_CACHE.get(cacheKey);`r`n        if (cached != null && (now - cached.tick()) <= HAZARD_CACHE_TTL_TICKS) {`r`n            HAZARD_CACHE_HITS.increment();`r`n            return cached.activeMachines();`r`n        }`r`n`r`n        HAZARD_CACHE_MISSES.increment();

        int minX = center.getX() - horizontalRadius;
        int maxX = center.getX() + horizontalRadius;
        int minY = center.getY() - verticalRadius;
        int maxY = center.getY() + verticalRadius;
        int minZ = center.getZ() - horizontalRadius;
        int maxZ = center.getZ() + horizontalRadius;

        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;

        int activeMachines = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof TechMachineBlockEntity machine)) {
                        continue;
                    }

                    BlockPos pos = be.getBlockPos();
                    if (pos.getX() < minX || pos.getX() > maxX
                            || pos.getY() < minY || pos.getY() > maxY
                            || pos.getZ() < minZ || pos.getZ() > maxZ) {
                        continue;
                    }

                    int maxEnergy = machine.getEnergyStorageExt().getMaxEnergyStored();
                    if (maxEnergy <= 0) {
                        continue;
                    }

                    if (machine.getEnergyStorageExt().getEnergyStored() > maxEnergy * 0.8F) {
                        activeMachines++;
                    }
                }
            }
        }

        pruneHazardCache(now);
        HAZARD_SCAN_CACHE.put(cacheKey, new HazardSnapshot(now, activeMachines));
        return activeMachines;
    }

    private static void pruneHazardCache(long now) {`r`n        if ((now - lastPruneTick) < HAZARD_CACHE_PRUNE_INTERVAL_TICKS && HAZARD_SCAN_CACHE.size() < HAZARD_CACHE_MAX_ENTRIES) {`r`n            return;`r`n        }`r`n`r`n        lastPruneTick = now;`r`n        int beforePrune = HAZARD_SCAN_CACHE.size();`r`n        HAZARD_SCAN_CACHE.entrySet().removeIf(entry -> (now - entry.getValue().tick()) > HAZARD_CACHE_TTL_TICKS);`r`n        int pruned = beforePrune - HAZARD_SCAN_CACHE.size();`r`n        if (pruned > 0) {`r`n            HAZARD_CACHE_PRUNED_ENTRIES.add(pruned);`r`n        }`r`n`r`n        if (HAZARD_SCAN_CACHE.size() <= HAZARD_CACHE_MAX_ENTRIES) {`r`n            return;`r`n        }`r`n`r`n        Iterator<String> keys = HAZARD_SCAN_CACHE.keySet().iterator();`r`n        long forcedEvictions = 0L;`r`n        while (HAZARD_SCAN_CACHE.size() > HAZARD_CACHE_MAX_ENTRIES && keys.hasNext()) {`r`n            keys.next();`r`n            keys.remove();`r`n            forcedEvictions++;`r`n        }`r`n`r`n        if (forcedEvictions > 0L) {`r`n            HAZARD_CACHE_FORCED_EVICTIONS.add(forcedEvictions);`r`n        }`r`n    }

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

    public static CacheStats cacheStats() {
        long hits = HAZARD_CACHE_HITS.sum();
        long misses = HAZARD_CACHE_MISSES.sum();
        return new CacheStats(
                hits,
                misses,
                HAZARD_CACHE_PRUNED_ENTRIES.sum(),
                HAZARD_CACHE_FORCED_EVICTIONS.sum(),
                HAZARD_SCAN_CACHE.size(),
                HAZARD_CACHE_TTL_TICKS,
                HAZARD_CACHE_MAX_ENTRIES
        );
    }

    public record CacheStats(
            long hits,
            long misses,
            long prunedEntries,
            long forcedEvictions,
            int cachedEntries,
            long ttlTicks,
            int maxEntries
    ) {
        public long lookups() {
            return hits + misses;
        }

        public int hitRatePercent() {
            long lookups = lookups();
            if (lookups <= 0L) {
                return 0;
            }
            return (int) Math.round((hits * 100.0D) / lookups);
        }
    }
    private boolean hasProtection(ServerPlayer player) {
        ItemStack chest = player.getInventory().armor.get(2);
        return !chest.isEmpty() && chest.getDescriptionId().contains("pioneer_chestplate");
    }

    private record HazardSnapshot(long tick, int activeMachines) {
    }
}




