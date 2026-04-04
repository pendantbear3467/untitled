package com.extremecraft.client;

import com.extremecraft.modules.runtime.ModuleCatalogClientState;
import com.extremecraft.platform.data.sync.client.PlatformDataClientState;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ClientRuntimeCleanupEvents {
    @SubscribeEvent
    public void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ModuleCatalogClientState.resetValidationState();
        PlatformDataClientState.resetValidationState();
    }
}