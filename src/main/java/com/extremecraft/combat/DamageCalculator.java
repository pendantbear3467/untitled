package com.extremecraft.combat;

public final class DamageCalculator {
    private DamageCalculator() {
    }

    public static DamageResult calculate(DamageContext context) {
        if (context == null) {
            return new DamageResult(
                    DamageType.PHYSICAL,
                    0.0F,
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

        float baseDamage = Math.max(0.0F, context.damageAmount());

        DamageModifiers modifiers = context.modifiers();
        float afterSkill = (baseDamage + modifiers.skillFlatBonus()) * modifiers.skillMultiplier();
        float afterWeapon = (afterSkill + modifiers.weaponFlatBonus()) * modifiers.weaponMultiplier();
        float afterAbility = (afterWeapon + modifiers.abilityFlatBonus()) * modifiers.abilityMultiplier();

        float criticalChance = clamp(context.criticalChance() + modifiers.criticalChanceBonus(), 0.0F, 1.0F);
        float criticalMultiplier = Math.max(1.0F, context.criticalMultiplier() + modifiers.criticalMultiplierBonus());
        boolean criticalHit = context.attacker() != null && context.attacker().getRandom().nextFloat() <= criticalChance;

        float afterCritical = criticalHit ? afterAbility * criticalMultiplier : afterAbility;

        float armorReduction = clamp(ArmorCalculator.calculateReduction(context.armorValue()) * modifiers.armorMultiplier(), 0.0F, 0.95F);
        float afterArmor = afterCritical * (1.0F - armorReduction);

        float resistanceReduction = clamp(context.resistance(context.damageType()) * modifiers.resistanceMultiplier(), 0.0F, 1.0F);
        float afterResistance = afterArmor * (1.0F - resistanceReduction);

        float statusMultiplier = Math.max(0.0F, modifiers.statusMultiplier());
        float finalDamage = Math.max(0.0F, afterResistance * statusMultiplier);

        return new DamageResult(
                context.damageType(),
                baseDamage,
                afterSkill,
                afterWeapon,
                afterAbility,
                criticalHit,
                criticalHit ? criticalMultiplier : 1.0F,
                afterCritical,
                armorReduction,
                afterArmor,
                resistanceReduction,
                afterResistance,
                statusMultiplier,
                finalDamage
        );
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
