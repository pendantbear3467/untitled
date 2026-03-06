package com.extremecraft.ability;

import java.util.List;
import java.util.Map;

public final class HealAbility implements Ability {
    @Override
    public String getId() {
        return "heal";
    }

    @Override
    public int getCooldown() {
        return 80;
    }

    @Override
    public void execute(AbilityContext context) {
        AbilityEffect heal = new AbilityEffect("heal", 8.0D, 0, 0, "", Map.of());
        AbilityEffect regen = new AbilityEffect("buff", 0.0D, 6, 0, "minecraft:regeneration", Map.of());
        AbilityExecutor.executeHeal(context, List.of(context.player()), heal);
        AbilityExecutor.executeBuff(context, List.of(context.player()), regen, false);
    }
}
