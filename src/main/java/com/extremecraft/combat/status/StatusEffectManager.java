package com.extremecraft.combat.status;

import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageType;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

public final class StatusEffectManager {
    private StatusEffectManager() {
    }

    private static final StatusEffect BURNING = new StatusEffect() {
        @Override
        public String id() {
            return "burning";
        }

        @Override
        public void apply(DamageContext context) {
            if (context.damageType() == DamageType.FIRE) {
                context.modifiers().multiplyStatus(1.20F);
            }
        }
    };

    private static final StatusEffect FROZEN = new StatusEffect() {
        @Override
        public String id() {
            return "frozen";
        }

        @Override
        public void apply(DamageContext context) {
            if (context.damageType() == DamageType.ICE) {
                context.modifiers().multiplyStatus(1.15F);
            }
            context.setArmorValue(Math.max(0.0F, context.armorValue() - 10.0F));
        }
    };

    private static final StatusEffect POISONED = new StatusEffect() {
        @Override
        public String id() {
            return "poisoned";
        }

        @Override
        public void apply(DamageContext context) {
            if (context.damageType() == DamageType.POISON) {
                context.modifiers().multiplyStatus(1.20F);
            }
        }
    };

    private static final StatusEffect BLEEDING = new StatusEffect() {
        @Override
        public String id() {
            return "bleeding";
        }

        @Override
        public void apply(DamageContext context) {
            if (context.damageType() == DamageType.BLEED || context.damageType() == DamageType.PHYSICAL) {
                context.modifiers().multiplyStatus(1.10F);
            }
        }
    };

    private static final StatusEffect SHOCK = new StatusEffect() {
        @Override
        public String id() {
            return "shock";
        }

        @Override
        public void apply(DamageContext context) {
            if (context.damageType() == DamageType.LIGHTNING || context.damageType() == DamageType.MAGIC) {
                context.modifiers().multiplyStatus(1.10F);
            }
        }
    };

    public static void applyToContext(DamageContext context) {
        if (context == null || context.target() == null) {
            return;
        }

        LivingEntity target = context.target();
        List<StatusEffect> effects = new java.util.ArrayList<>();

        if (target.isOnFire()) {
            effects.add(BURNING);
        }
        if (target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            effects.add(FROZEN);
        }
        if (target.hasEffect(MobEffects.POISON)) {
            effects.add(POISONED);
        }
        if (target.hasEffect(MobEffects.WEAKNESS)) {
            effects.add(BLEEDING);
        }
        if (target.hasEffect(MobEffects.GLOWING)) {
            effects.add(SHOCK);
        }

        for (StatusEffect effect : effects) {
            effect.apply(context);
        }
    }
}
