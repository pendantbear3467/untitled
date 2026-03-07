package com.extremecraft.machine.sync;

import com.extremecraft.core.ECConstants;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Keeps runtime machine sync caches bounded across server restarts in long-lived dev sessions.
 */
@Mod.EventBusSubscriber(modid = ECConstants.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MachineSyncLifecycleEvents {
    private MachineSyncLifecycleEvents() {
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MachineSyncIndex.clear();
    }
}
