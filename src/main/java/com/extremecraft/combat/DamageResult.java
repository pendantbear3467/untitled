package com.extremecraft.combat;

public record DamageResult(
        DamageType damageType,
        float baseDamage,
        float afterSkill,
        float afterWeapon,
        float afterAbility,
        boolean criticalHit,
        float criticalMultiplier,
        float afterCritical,
        float armorReduction,
        float afterArmor,
        float resistanceReduction,
        float afterResistance,
        float statusMultiplier,
        float finalDamage
) {
}
