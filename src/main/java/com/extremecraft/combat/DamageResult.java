package com.extremecraft.combat;

/**
 * Immutable damage breakdown emitted by {@link DamageCalculator}.
 *
 * <p>Used for final hurt amount and for observability in debug overlays/events.</p>
 */
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
