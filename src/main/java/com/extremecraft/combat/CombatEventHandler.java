package com.extremecraft.combat;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge event adapter that routes vanilla hurt events into the unified combat engine.
 */
public class CombatEventHandler {
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        CombatEngine.processLivingHurtEvent(event);
    }
}
