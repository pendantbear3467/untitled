package com.extremecraft.server.task;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerDeferredWorkEvents {
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null || event.getServer().overworld() == null) {
            return;
        }

        long now = event.getServer().overworld().getGameTime();
        ServerDeferredWorkQueue.tick(now, 256);
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        ServerDeferredWorkQueue.clear();
    }
}
