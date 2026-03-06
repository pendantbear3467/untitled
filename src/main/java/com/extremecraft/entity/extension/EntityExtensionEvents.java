package com.extremecraft.entity.extension;

import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class EntityExtensionEvents {
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!EntityExtensionRegistry.hasAnyExtensions()) {
            return;
        }

        EntityExtensionRegistry.runServerTick(event.getEntity());
    }
}
