package com.extremecraft.ability;

import java.util.Map;

public final class FireballAbility implements Ability {
    @Override
    public String getId() {
        return "fireball";
    }

    @Override
    public int getCooldown() {
        return 60;
    }

    @Override
    public void execute(AbilityContext context) {
        AbilityEffect projectile = new AbilityEffect("projectile", 6.0D, 0, 0, "", Map.of("speed", 2.6D));
        AbilityExecutor.executeProjectile(context, projectile);
    }
}
