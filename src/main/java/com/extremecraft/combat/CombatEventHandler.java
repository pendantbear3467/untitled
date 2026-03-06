package com.extremecraft.combat;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CombatEventHandler {
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        CombatEngine.processLivingHurtEvent(event);
    }
}
