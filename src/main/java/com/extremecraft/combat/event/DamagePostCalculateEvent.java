package com.extremecraft.combat.event;

import com.extremecraft.combat.DamageContext;
import com.extremecraft.combat.DamageResult;
import net.minecraftforge.eventbus.api.Event;

public class DamagePostCalculateEvent extends Event {
    private final DamageContext context;
    private final DamageResult result;

    public DamagePostCalculateEvent(DamageContext context, DamageResult result) {
        this.context = context;
        this.result = result;
    }

    public DamageContext context() {
        return context;
    }

    public DamageResult result() {
        return result;
    }
}
