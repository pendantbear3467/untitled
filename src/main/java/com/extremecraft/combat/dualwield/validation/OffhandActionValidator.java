package com.extremecraft.combat.dualwield.validation;

import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.server.DwServerTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central validation and anti-spam checks for server-side offhand actions.
 */
public final class OffhandActionValidator {
    private static final Map<UUID, TimingState> TIMING_STATE = new ConcurrentHashMap<>();

    private OffhandActionValidator() {
    }

    public static ValidationResult validate(ServerPlayer player, OffhandActionC2S packet) {
        if (player == null || packet == null || player.isSpectator()) {
            return ValidationResult.DENIED;
        }

        return switch (packet.action()) {
            case HOLD_ABORT_BREAK -> ValidationResult.allow();
            case ATTACK_ENTITY -> validateAttack(player, packet.entityId());
            case USE_ITEM -> validateUseItem(player);
            case USE_ON_BLOCK -> validateUseOnBlock(player, packet.pos());
            case TAP_BREAK -> validateTapBreak(player, packet.pos());
            case HOLD_START_BREAK -> validateHoldStartBreak(player, packet.pos(), packet.face());
        };
    }

    public static boolean canHandle(ServerPlayer player, OffhandActionC2S packet) {
        return validate(player, packet).accepted();
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

        return target.isAttackable()
                && !target.skipAttackInteraction(player)
                && player.distanceToSqr(target) <= 36.0D
                && player.hasLineOfSight(target);
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

    public static void clearPlayer(ServerPlayer player) {
        if (player != null) {
            TIMING_STATE.remove(player.getUUID());
        }
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            TIMING_STATE.remove(playerId);
        }
    }

    private static ValidationResult validateAttack(ServerPlayer player, int entityId) {
        if (!hasUsableOffhand(player) || !canAttackEntity(player.serverLevel(), player, entityId)) {
            return ValidationResult.DENIED;
        }

        long now = player.serverLevel().getGameTime();
        TimingState timing = TIMING_STATE.computeIfAbsent(player.getUUID(), ignored -> new TimingState());
        int cooldownTicks = attackCooldownTicks(player);
        return timing.tryAttack(now, cooldownTicks);
    }

    private static ValidationResult validateUseItem(ServerPlayer player) {
        if (!hasUsableOffhand(player)) {
            return ValidationResult.DENIED;
        }

        return allowLane(player, TimingState::tryItemUse);
    }

    private static ValidationResult validateUseOnBlock(ServerPlayer player, BlockPos pos) {
        if (!hasUsableOffhand(player) || !canInteractWithBlock(player, pos)) {
            return ValidationResult.DENIED;
        }

        return allowLane(player, TimingState::tryBlockUse);
    }

    private static ValidationResult validateTapBreak(ServerPlayer player, BlockPos pos) {
        if (!hasUsableOffhand(player) || !canInteractWithBlock(player, pos) || player.serverLevel().isEmptyBlock(pos)) {
            return ValidationResult.DENIED;
        }

        return allowLane(player, TimingState::tryBreakTap);
    }

    private static ValidationResult validateHoldStartBreak(ServerPlayer player, BlockPos pos, net.minecraft.core.Direction face) {
        if (!hasUsableOffhand(player)
                || !canInteractWithBlock(player, pos)
                || player.serverLevel().isEmptyBlock(pos)
                || !DwServerTicker.canStartOffhandBreak(player, pos, face)) {
            return ValidationResult.DENIED;
        }

        return allowLane(player, TimingState::tryBreakStart);
    }

    private static ValidationResult allowLane(ServerPlayer player, LaneGate gate) {
        long now = player.serverLevel().getGameTime();
        TimingState timing = TIMING_STATE.computeIfAbsent(player.getUUID(), ignored -> new TimingState());
        return gate.tryAllow(timing, now) ? ValidationResult.allow() : ValidationResult.DENIED;
    }

    private static int attackCooldownTicks(ServerPlayer player) {
        double liveAttackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
        double attackSpeed = liveAttackSpeed
                - attributeContribution(player.getMainHandItem(), Attributes.ATTACK_SPEED, EquipmentSlot.MAINHAND)
                + preferredWeaponContribution(player.getOffhandItem(), Attributes.ATTACK_SPEED);
        return Math.max(1, Mth.ceil((float) (20.0D / Math.max(0.1D, attackSpeed))));
    }

    private static double preferredWeaponContribution(ItemStack stack, Attribute attribute) {
        double offhand = attributeContribution(stack, attribute, EquipmentSlot.OFFHAND);
        if (offhand != 0.0D) {
            return offhand;
        }
        return attributeContribution(stack, attribute, EquipmentSlot.MAINHAND);
    }

    private static double attributeContribution(ItemStack stack, Attribute attribute, EquipmentSlot slot) {
        if (stack.isEmpty()) {
            return 0.0D;
        }

        Collection<AttributeModifier> modifiers = stack.getAttributeModifiers(slot).get(attribute);
        if (modifiers.isEmpty()) {
            return 0.0D;
        }

        double additive = 0.0D;
        double multiplyBase = 0.0D;
        double multiplyTotal = 1.0D;
        for (AttributeModifier modifier : modifiers) {
            switch (modifier.getOperation()) {
                case ADDITION -> additive += modifier.getAmount();
                case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal *= 1.0D + modifier.getAmount();
            }
        }

        return additive * (1.0D + multiplyBase) * multiplyTotal;
    }

    public record ValidationResult(boolean accepted, float attackStrengthScale) {
        private static final ValidationResult DENIED = new ValidationResult(false, 0.0F);
        private static final ValidationResult DEFAULT_ALLOWED = new ValidationResult(true, 1.0F);

        public static ValidationResult allow() {
            return DEFAULT_ALLOWED;
        }

        public static ValidationResult allow(float attackStrengthScale) {
            return new ValidationResult(true, Mth.clamp(attackStrengthScale, 0.0F, 1.0F));
        }
    }

    @FunctionalInterface
    private interface LaneGate {
        boolean tryAllow(TimingState timing, long now);
    }

    private static final class TimingState {
        private long lastAttackTick = Long.MIN_VALUE / 4L;
        private long lastItemUseTick = Long.MIN_VALUE / 4L;
        private long lastBlockUseTick = Long.MIN_VALUE / 4L;
        private long lastBreakTapTick = Long.MIN_VALUE / 4L;
        private long lastBreakStartTick = Long.MIN_VALUE / 4L;

        synchronized ValidationResult tryAttack(long now, int cooldownTicks) {
            float strengthScale = attackStrengthScale(now, cooldownTicks);
            if ((now - lastAttackTick) < Math.max(1, cooldownTicks)) {
                return ValidationResult.DENIED;
            }

            lastAttackTick = now;
            return ValidationResult.allow(strengthScale);
        }

        synchronized boolean tryItemUse(long now) {
            return tryAllow(now, 1, Lane.ITEM_USE);
        }

        synchronized boolean tryBlockUse(long now) {
            return tryAllow(now, 1, Lane.BLOCK_USE);
        }

        synchronized boolean tryBreakTap(long now) {
            return tryAllow(now, 1, Lane.BREAK_TAP);
        }

        synchronized boolean tryBreakStart(long now) {
            return tryAllow(now, 1, Lane.BREAK_START);
        }

        private boolean tryAllow(long now, int minDelta, Lane lane) {
            long previous = switch (lane) {
                case ITEM_USE -> lastItemUseTick;
                case BLOCK_USE -> lastBlockUseTick;
                case BREAK_TAP -> lastBreakTapTick;
                case BREAK_START -> lastBreakStartTick;
            };

            if ((now - previous) < minDelta) {
                return false;
            }

            switch (lane) {
                case ITEM_USE -> lastItemUseTick = now;
                case BLOCK_USE -> lastBlockUseTick = now;
                case BREAK_TAP -> lastBreakTapTick = now;
                case BREAK_START -> lastBreakStartTick = now;
            }
            return true;
        }

        private float attackStrengthScale(long now, int cooldownTicks) {
            if (lastAttackTick <= (Long.MIN_VALUE / 8L)) {
                return 1.0F;
            }

            return Mth.clamp((float) (now - lastAttackTick) / (float) Math.max(1, cooldownTicks), 0.0F, 1.0F);
        }
    }

    private enum Lane {
        ITEM_USE,
        BLOCK_USE,
        BREAK_TAP,
        BREAK_START
    }
}
