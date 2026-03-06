package com.extremecraft.ability;

public final class DashAbility implements Ability {
    @Override
    public String getId() {
        return "dash";
    }

    @Override
    public int getCooldown() {
        return 40;
    }

    @Override
    public void execute(AbilityContext context) {
        double distance = context.definition() == null ? 6.0D : Math.max(3.0D, context.definition().range() * 0.5D);
        AbilityExecutor.executeMovement(context, distance);
    }
}
