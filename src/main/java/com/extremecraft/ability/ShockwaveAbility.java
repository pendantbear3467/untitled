package com.extremecraft.ability;

import java.util.List;
import java.util.Map;

public final class ShockwaveAbility implements Ability {
    @Override
    public String getId() {
        return "shockwave";
    }

    @Override
    public int getCooldown() {
        return 100;
    }

    @Override
    public void execute(AbilityContext context) {
        double radius = context.definition() == null ? 5.0D : Math.max(2.0D, context.definition().radius());
        AbilityEffect damage = new AbilityEffect("damage", 8.0D, 0, 0, "", Map.of());
        AbilityEffect debuff = new AbilityEffect("debuff", 0.0D, 4, 0, "minecraft:slowness", Map.of());
        AbilityExecutor.executeAreaEffect(context, radius, List.of(damage, debuff));
    }
}
