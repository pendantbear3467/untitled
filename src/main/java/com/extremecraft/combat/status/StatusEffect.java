package com.extremecraft.combat.status;

import com.extremecraft.combat.DamageContext;

public interface StatusEffect {
    String id();

    void apply(DamageContext context);
}
