package com.extremecraft.foundation;

import com.extremecraft.config.ECFoundationConfig;
import com.extremecraft.dev.validation.ECTickProfiler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ECDestructiveEffectService {
    private static final Map<String, ArrayDeque<PendingPulse>> PENDING_PULSES = new ConcurrentHashMap<>();

    private ECDestructiveEffectService() {
    }

    public static void queueSphere(ServerLevel level, BlockPos center, int requestedRadius, int requestedBudget, String cause) {
        if (level == null || center == null) {
            return;
        }

        long start = System.nanoTime();
        int radius = Math.max(0, Math.min(requestedRadius, ECFoundationConfig.catastrophicMaxRadius()));
        int budget = Math.max(0, Math.min(requestedBudget, ECFoundationConfig.catastrophicMaxAffectedBlocks()));
        applyEntityShockwave(level, center, radius, cause);

        if (!ECFoundationConfig.enableWorldEdits() || radius <= 0 || budget <= 0) {
            record(start);
            return;
        }

        ArrayDeque<BlockPos> blocks = new ArrayDeque<>();
        int radiusSq = radius * radius;
        for (BlockPos target : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (blocks.size() >= budget) {
                break;
            }

            if (center.distSqr(target) > radiusSq) {
                continue;
            }

            BlockState state = level.getBlockState(target);
            if (!canDestroy(level, target, state)) {
                continue;
            }

            blocks.add(target.immutable());
        }

        if (!blocks.isEmpty()) {
            PENDING_PULSES.computeIfAbsent(levelKey(level), ignored -> new ArrayDeque<>())
                    .add(new PendingPulse(blocks));
        }
        record(start);
    }

    public static void tickLevel(ServerLevel level) {
        ArrayDeque<PendingPulse> pulses = PENDING_PULSES.get(levelKey(level));
        if (pulses == null || pulses.isEmpty()) {
            return;
        }

        long start = System.nanoTime();
        int remaining = Math.max(1, ECFoundationConfig.destructivePulseBatchSize());
        while (remaining > 0 && !pulses.isEmpty()) {
            PendingPulse pulse = pulses.peek();
            BlockPos next = pulse.blocks.pollFirst();
            if (next == null) {
                pulses.poll();
                continue;
            }

            if (canDestroy(level, next, level.getBlockState(next))) {
                level.destroyBlock(next, false);
            }
            remaining--;

            if (pulse.blocks.isEmpty()) {
                pulses.poll();
            }
        }

        if (pulses.isEmpty()) {
            PENDING_PULSES.remove(levelKey(level));
        }
        record(start);
    }

    private static void applyEntityShockwave(ServerLevel level, BlockPos center, int radius, String cause) {
        double damageRadius = Math.max(2.0D, radius + 2.0D);
        AABB area = new AABB(center).inflate(damageRadius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            double distance = Math.max(1.0D, Math.sqrt(entity.blockPosition().distSqr(center)));
            float damage = (float) Math.max(2.0D, (damageRadius * 2.0D) / distance);
            entity.hurt(level.damageSources().magic(), damage);
            if (cause != null && (cause.contains("nuke") || cause.contains("meltdown") || cause.contains("fire"))) {
                entity.setSecondsOnFire(Math.max(entity.getRemainingFireTicks() / 20, radius / 2));
            }
        }
    }

    private static boolean canDestroy(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.END_PORTAL) || state.is(Blocks.BARRIER)) {
            return false;
        }
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private static String levelKey(ServerLevel level) {
        return level.dimension().location().toString();
    }

    private static void record(long startNanos) {
        if (ECFoundationConfig.isProfilerEnabled()) {
            ECTickProfiler.record("destructive_effects", System.nanoTime() - startNanos);
        }
    }

    private record PendingPulse(ArrayDeque<BlockPos> blocks) {
    }
}
