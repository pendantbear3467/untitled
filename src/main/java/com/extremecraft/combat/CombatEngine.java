package com.extremecraft.combat;

import com.extremecraft.combat.event.DamagePostCalculateEvent;
import com.extremecraft.combat.event.DamagePreCalculateEvent;
import com.extremecraft.combat.status.StatusEffectManager;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class CombatEngine {
    /**
     * Central combat pipeline for both vanilla and data-driven ability damage.
     * <p>
     * Damage requests from abilities/spells feed into {@link #applyDamage(DamageContext)}, while
     * vanilla hurt events are reconciled by {@code processLivingHurtEvent} so all
     * combat modifiers, resistances, and status effects are resolved through one path.
     */
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadLocal<DamageContext> PENDING_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Integer> REENTRANCY_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Set<Integer>> ACTIVE_TARGET_IDS = ThreadLocal.withInitial(HashSet::new);

    private static final int MAX_REENTRANCY_DEPTH = 8;

    private CombatEngine() {
    }

    public static DamageResult applyDamage(DamageContext context) {
        if (context == null || context.target() == null || context.damageAmount() <= 0.0F) {
            return DamageCalculator.calculate(context);
        }

        if (context.target().level().isClientSide) {
            return previewDamage(context);
        }

        if (!enterDamageScope()) {
            LOGGER.warn("[CombatEngine] Rejected damage application due to excessive recursion depth");
            return previewDamage(context);
        }

        int targetId = context.target().getId();
        if (!enterTargetScope(targetId)) {
            LOGGER.warn("[CombatEngine] Rejected damage application due to recursive target processing");
            exitDamageScope();
            return previewDamage(context);
        }

        try {
            PENDING_CONTEXT.set(context);
            DamageSource source = resolveDamageSource(context);
            context.target().hurt(source, context.damageAmount());
        } catch (Exception ex) {
            LOGGER.error("CombatEngine failed to apply damage", ex);
        } finally {
            PENDING_CONTEXT.remove();
            exitTargetScope(targetId);
            exitDamageScope();
        }

        return previewDamage(context);
    }

    public static DamageResult previewDamage(DamageContext context) {
        if (context == null) {
            return DamageCalculator.calculate(null);
        }

        if (MinecraftForge.EVENT_BUS.post(new DamagePreCalculateEvent(context))) {
            return new DamageResult(
                    context.damageType(),
                    context.damageAmount(),
                    0.0F,
                    0.0F,
                    0.0F,
                    false,
                    1.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    0.0F,
                    1.0F,
                    0.0F
            );
        }

        StatusEffectManager.applyToContext(context);
        DamageResult result = DamageCalculator.calculate(context);
        MinecraftForge.EVENT_BUS.post(new DamagePostCalculateEvent(context, result));
        return result;
    }

    public static void processLivingHurtEvent(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event == null || event.getEntity() == null || event.getSource() == null || event.getAmount() <= 0.0F) {
            return;
        }

        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (currentDepth() > MAX_REENTRANCY_DEPTH) {
            event.setAmount(Math.max(0.0F, event.getAmount()));
            return;
        }

        LivingEntity target = event.getEntity();
        DamageContext context = PENDING_CONTEXT.get();
        if (context == null || context.target() != target) {
            context = fromLivingHurtEvent(event);
        }

        DamageResult result = previewDamage(context);
        event.setAmount(Math.max(0.0F, result.finalDamage()));
    }

    public static DamageContext fromLivingHurtEvent(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        LivingEntity target = event.getEntity();

        DamageContext context = DamageContext.builder()
                .attacker(attacker)
                .target(target)
                .damageAmount(event.getAmount())
                .damageType(DamageType.infer(event.getSource()))
                .criticalChance(criticalChance(attacker))
                .criticalMultiplier(criticalMultiplier(attacker))
                .weaponSource(attacker == null ? ItemStack.EMPTY : attacker.getMainHandItem())
                .armorValue(target.getArmorValue())
                .build();

        applyBaseModifiers(context, attacker, target);
        applyResistances(context, target);
        return context;
    }

    private static void applyBaseModifiers(DamageContext context, LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof ServerPlayer serverPlayer) {
            PlayerStatsApi.get(serverPlayer).ifPresent(stats -> {
                context.modifiers().addSkillFlatBonus(stats.meleeDamageBonus());
                context.modifiers().multiplySkill(stats.damageMultiplier());
            });

            float weaponDamage = weaponDamage(attacker);
            if (weaponDamage > 0.0F) {
                context.modifiers().addWeaponFlatBonus(weaponDamage);
            }
        }

        if (target instanceof ServerPlayer targetPlayer) {
            PlayerStatsApi.get(targetPlayer).ifPresent(stats -> {
                float defenseMultiplier = Math.max(0.1F, 1.0F / Math.max(0.1F, stats.damageMultiplier()));
                context.modifiers().multiplyStatus(defenseMultiplier);
            });
        }
    }

    private static void applyResistances(DamageContext context, LivingEntity target) {
        if (target instanceof ServerPlayer player) {
            PlayerStatsApi.get(player).ifPresent(stats -> {
                context.setResistance(DamageType.FIRE, stats.equipmentModifier("fire_resistance"));
                context.setResistance(DamageType.MAGIC, stats.equipmentModifier("magic_resistance"));
                context.setResistance(DamageType.POISON, stats.equipmentModifier("poison_resistance"));
            });
        }
    }

    private static float criticalChance(LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            return PlayerStatsApi.get(player).map(PlayerStatsCapability::critChance).orElse(0.0F);
        }
        return 0.0F;
    }

    private static float criticalMultiplier(LivingEntity attacker) {
        if (attacker instanceof ServerPlayer player) {
            return PlayerStatsApi.get(player).map(PlayerStatsCapability::critDamage).orElse(1.5F);
        }
        return 1.5F;
    }

    private static float weaponDamage(LivingEntity attacker) {
        ItemStack weapon = attacker.getMainHandItem();
        if (weapon.isEmpty()) {
            return 0.0F;
        }

        Collection<AttributeModifier> modifiers = weapon.getAttributeModifiers(EquipmentSlot.MAINHAND)
                .get(Attributes.ATTACK_DAMAGE);
        float total = 0.0F;
        for (AttributeModifier modifier : modifiers) {
            total += (float) modifier.getAmount();
        }
        return Math.max(0.0F, total);
    }

    private static DamageSource resolveDamageSource(DamageContext context) {
        if (context.attacker() instanceof ServerPlayer player) {
            return player.damageSources().playerAttack(player);
        }
        if (context.attacker() != null) {
            return context.attacker().damageSources().mobAttack(context.attacker());
        }
        return context.target().damageSources().generic();
    }

    private static boolean enterTargetScope(int targetId) {
        return ACTIVE_TARGET_IDS.get().add(targetId);
    }

    private static void exitTargetScope(int targetId) {
        Set<Integer> activeTargets = ACTIVE_TARGET_IDS.get();
        activeTargets.remove(targetId);
        if (activeTargets.isEmpty()) {
            ACTIVE_TARGET_IDS.remove();
        }
    }

    private static boolean enterDamageScope() {
        int current = REENTRANCY_DEPTH.get();
        if (current >= MAX_REENTRANCY_DEPTH) {
            return false;
        }

        REENTRANCY_DEPTH.set(current + 1);
        return true;
    }

    private static void exitDamageScope() {
        int next = REENTRANCY_DEPTH.get() - 1;
        if (next <= 0) {
            REENTRANCY_DEPTH.remove();
            return;
        }

        REENTRANCY_DEPTH.set(next);
    }

    private static int currentDepth() {
        return REENTRANCY_DEPTH.get();
    }
}




