package com.extremecraft.combat.dualwield.service;

import com.extremecraft.combat.dualwield.validation.OffhandActionValidator;
import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.server.DwServerTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes validated offhand actions on the server thread.
 */
public final class OffhandActionExecutor {
    private static final Logger LOGGER = Logger.getLogger("ExtremeCraft");
    private static final float ATTACK_EXHAUSTION = 0.1F;

    private OffhandActionExecutor() {
    }

    public static void execute(ServerPlayer player, ServerLevel level, OffhandActionC2S packet,
                               OffhandActionValidator.ValidationResult validation) {
        if (player == null || level == null || player.level().isClientSide()) {
            return;
        }

        try {
            switch (packet.action()) {
                case ATTACK_ENTITY -> attackEntity(player, level, packet.entityId(), validation.attackStrengthScale());
                case USE_ITEM -> useItem(player, level);
                case USE_ON_BLOCK -> useOnBlock(player, level, packet.pos(), packet.face());
                case TAP_BREAK -> tapBreak(player, packet.pos(), packet.face());
                case HOLD_START_BREAK -> holdStartBreak(player, packet.pos(), packet.face());
                case HOLD_ABORT_BREAK -> DwServerTicker.abortOffhandBreak(player, true);
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Failed to execute offhand action " + packet.action(), ex);
        }
    }

    public static void attackEntity(ServerPlayer player, ServerLevel level, int entityId, float attackStrengthScale) {
        Entity target = level.getEntity(entityId);
        if (!isAttackTargetValid(player, target)) {
            return;
        }

        DwServerTicker.abortOffhandBreak(player, true);
        if (performExplicitOffhandAttack(player, target, attackStrengthScale)) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    public static void attackEntityWithLegacyCharge(ServerPlayer player, Entity target) {
        if (target == null || player == null || player.serverLevel().isClientSide()) {
            return;
        }

        if (isAttackTargetValid(player, target) && performExplicitOffhandAttack(player, target, 1.0F)) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    private static void useItem(ServerPlayer player, ServerLevel level) {
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        DwServerTicker.abortOffhandBreak(player, true);
        InteractionResult result = player.gameMode.useItem(player, level, off, InteractionHand.OFF_HAND);
        if (result.consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    private static void useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        DwServerTicker.abortOffhandBreak(player, true);
        BlockHitResult hit = centeredFaceHit(pos, face);
        InteractionResult result = player.gameMode.useItemOn(player, level, off, InteractionHand.OFF_HAND, hit);
        if (result.consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
            return;
        }

        if (result != InteractionResult.PASS) {
            return;
        }

        InteractionResult fallback = player.gameMode.useItem(player, level, off, InteractionHand.OFF_HAND);
        if (fallback.consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    private static void tapBreak(ServerPlayer player, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        DwServerTicker.abortOffhandBreak(player, true);
        callHandleBlockBreakAction(player, pos, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        callHandleBlockBreakAction(player, pos, face, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
        player.swing(InteractionHand.OFF_HAND, true);
    }

    private static void holdStartBreak(ServerPlayer player, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        DwServerTicker.startOffhandBreak(player, pos, face);
        player.swing(InteractionHand.OFF_HAND, true);
    }

    public static void callHandleBlockBreakAction(ServerPlayer player, BlockPos pos, Direction face,
                                                  ServerboundPlayerActionPacket.Action action) {
        Objects.requireNonNull(player);
        player.gameMode.handleBlockBreakAction(pos, action, face, player.level().getMaxBuildHeight(), 0);
    }

    private static BlockHitResult centeredFaceHit(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 hit = center.add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        return new BlockHitResult(hit, face, pos, false);
    }

    private static boolean performExplicitOffhandAttack(ServerPlayer player, Entity target, float attackStrengthScale) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || !isAttackTargetValid(player, target)) {
            return false;
        }

        float charge = Mth.clamp(attackStrengthScale, 0.0F, 1.0F);
        LivingEntity livingTarget = target instanceof LivingEntity living ? living : null;

        float baseDamage = (float) adjustedAttributeValue(player, Attributes.ATTACK_DAMAGE, offhand);
        float scaledDamage = baseDamage * (0.2F + charge * charge * 0.8F);
        float enchantmentDamage = livingTarget == null ? 0.0F : EnchantmentHelper.getDamageBonus(offhand, livingTarget.getMobType()) * charge;
        boolean fullyCharged = charge > 0.9F;
        boolean criticalHit = fullyCharged && isCriticalHit(player, target);
        if (criticalHit) {
            scaledDamage *= 1.5F;
        }

        float totalDamage = scaledDamage + enchantmentDamage;
        if (totalDamage <= 0.0F) {
            return false;
        }

        int fireAspect = livingTarget == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, offhand);
        boolean primedFire = fireAspect > 0 && !target.isOnFire();
        if (primedFire) {
            target.setSecondsOnFire(1);
        }

        boolean hurt = target.hurt(player.damageSources().playerAttack(player), totalDamage);
        if (!hurt) {
            if (primedFire) {
                target.clearFire();
            }
            return false;
        }

        int knockback = Mth.floor(player.getAttributeValue(Attributes.ATTACK_KNOCKBACK))
            + EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, offhand);
        if (player.isSprinting() && fullyCharged) {
            knockback++;
            player.setSprinting(false);
        }
        if (knockback > 0 && livingTarget != null) {
            double yawRadians = player.getYRot() * (Math.PI / 180.0D);
            livingTarget.knockback(knockback * 0.5D, Mth.sin((float) yawRadians), -Mth.cos((float) yawRadians));
        }

        if (livingTarget != null) {
            offhand.hurtEnemy(livingTarget, player);
            EnchantmentHelper.doPostHurtEffects(livingTarget, player);
        }
        EnchantmentHelper.doPostDamageEffects(player, target);

        if (fireAspect > 0) {
            target.setSecondsOnFire(fireAspect * 4);
        }
        if (criticalHit) {
            player.crit(target);
        } else if (enchantmentDamage > 0.0F) {
            player.magicCrit(target);
        }

        maybeDisableShield(player, offhand, target, fullyCharged);
        player.setLastHurtMob(target);
        player.awardStat(Stats.DAMAGE_DEALT, Math.round(totalDamage * 10.0F));
        player.causeFoodExhaustion(ATTACK_EXHAUSTION);
        return true;
    }

    private static boolean isAttackTargetValid(ServerPlayer player, Entity target) {
        return player != null
                && target != null
                && target != player
                && target.isAlive()
                && target.isAttackable()
                && !target.isRemoved()
                && !target.skipAttackInteraction(player)
                && player.distanceToSqr(target) <= 36.0D
                && player.hasLineOfSight(target)
                && !player.isSpectator();
    }

    private static boolean isCriticalHit(ServerPlayer player, Entity target) {
        return target instanceof LivingEntity
                && !player.onGround()
                && player.fallDistance > 0.0F
                && !player.isInWater()
                && !player.onClimbable()
                && !player.isSprinting()
                && !player.isPassenger();
    }

    private static void maybeDisableShield(ServerPlayer player, ItemStack offhand, Entity target, boolean fullyCharged) {
        if (!fullyCharged || !(target instanceof Player defendingPlayer) || !defendingPlayer.isBlocking()) {
            return;
        }

        if (offhand.getItem() instanceof AxeItem) {
            defendingPlayer.disableShield(true);
        }
    }

    private static double adjustedAttributeValue(ServerPlayer player, Attribute attribute, ItemStack replacementMainhandEquivalent) {
        double liveValue = player.getAttributeValue(attribute);
        return liveValue
                - attributeContribution(player.getMainHandItem(), attribute, EquipmentSlot.MAINHAND)
                + preferredWeaponContribution(replacementMainhandEquivalent, attribute);
    }

    private static double preferredWeaponContribution(ItemStack stack, Attribute attribute) {
        double offhandContribution = attributeContribution(stack, attribute, EquipmentSlot.OFFHAND);
        if (offhandContribution != 0.0D) {
            return offhandContribution;
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
}

