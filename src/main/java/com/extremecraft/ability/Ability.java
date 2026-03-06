package com.extremecraft.ability;

public interface Ability {
    String getId();

    int getCooldown();

    void execute(AbilityContext context);
}
