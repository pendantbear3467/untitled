package com.extremecraft.combat.dualwield.validation;

import com.extremecraft.net.OffhandActionC2S;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central validation and anti-spam checks for server-side offhand actions.
 */
public final class OffhandActionValidator {
    private static final Map<UUID, Integer> LAST_ACTION_TICK = new ConcurrentHashMap<>();

    private OffhandActionValidator() {
    }

    public static boolean canHandle(ServerPlayer player, OffhandActionC2S packet) {
        if (player == null || packet == null || player.isSpectator()) {
            return false;
        }

        if (!passesRateLimit(player, packet.action())) {
            return false;
        }

        return switch (packet.action()) {
            case HOLD_ABORT_BREAK -> true;
            case USE_ITEM, USE_ON_BLOCK, TAP_BREAK, HOLD_START_BREAK, ATTACK_ENTITY -> hasUsableOffhand(player);
        } && switch (packet.action()) {
            case ATTACK_ENTITY -> canAttackEntity(player.serverLevel(), player, packet.entityId());
            case USE_ITEM -> true;
            case USE_ON_BLOCK, TAP_BREAK, HOLD_START_BREAK -> canInteractWithBlock(player, packet.pos());
            case HOLD_ABORT_BREAK -> true;
        };
    }

    public static boolean hasUsableOffhand(ServerPlayer player) {
        ItemStack off = player.getOffhandItem();
        return !off.isEmpty();
    }

    public static boolean canAttackEntity(ServerLevel level, ServerPlayer player, int entityId) {
        if (entityId < 0) {
            return false;
        }

        Entity target = level.getEntity(entityId);
        if (target == null || target == player || target.isRemoved() || !target.isAlive()) {
            return false;
        }

        return player.distanceToSqr(target) <= 36.0D && player.hasLineOfSight(target);
    }

    public static boolean canInteractWithBlock(ServerPlayer player, BlockPos pos) {
        if (pos == null) {
            return false;
        }

        return withinReach(player, pos) && player.serverLevel().isLoaded(pos);
    }

    public static boolean withinReach(ServerPlayer player, BlockPos pos) {
        return player.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 36.0D;
    }

    private static boolean passesRateLimit(ServerPlayer player, OffhandActionC2S.Action action) {
        int now = player.tickCount;
        int minDelta = minTickDelta(action);
        int last = LAST_ACTION_TICK.getOrDefault(player.getUUID(), Integer.MIN_VALUE / 2);

        if ((now - last) < minDelta) {
            return false;
        }

        LAST_ACTION_TICK.put(player.getUUID(), now);
        return true;
    }

    private static int minTickDelta(OffhandActionC2S.Action action) {
        return switch (action) {
            case ATTACK_ENTITY -> 2;
            case USE_ITEM, USE_ON_BLOCK -> 1;
            case TAP_BREAK, HOLD_START_BREAK, HOLD_ABORT_BREAK -> 1;
        };
    }
}
