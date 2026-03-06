package com.extremecraft.combat.event;

import com.extremecraft.combat.DamageContext;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class DamagePreCalculateEvent extends Event {
    private final DamageContext context;

    public DamagePreCalculateEvent(DamageContext context) {
        this.context = context;
    }

    public DamageContext context() {
        return context;
    }
}
