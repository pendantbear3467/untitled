package com.extremecraft.client;

import com.extremecraft.client.gui.machine.TechMachineScreen;
import com.extremecraft.client.gui.player.AbilityBarOverlay;
import com.extremecraft.client.gui.player.InventoryButtonInjector;
import com.extremecraft.client.gui.player.InventoryXpOverlay;
import com.extremecraft.client.render.entity.ModEntityRenderers;
import com.extremecraft.future.registry.TechMenuTypes;
import com.extremecraft.gui.PulverizerScreen;
import com.extremecraft.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only lifecycle wiring isolated from common bootstrap.
 */
public final class ClientLifecycleBridge {
    private ClientLifecycleBridge() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientLifecycleBridge::onClientSetup);
        modBus.addListener(DwKeybinds::onRegisterKeyMappings);
        modBus.addListener(ExtremeCraftKeybinds::onRegisterKeyMappings);
        modBus.addListener(ModEntityRenderers::registerRenderers);
        modBus.addListener(ModEntityRenderers::registerLayerDefinitions);
    }

    private static void onClientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PULVERIZER_MENU.get(), PulverizerScreen::new);
            MenuScreens.register(TechMenuTypes.TECH_MACHINE.get(), TechMachineScreen::new);
            MinecraftForge.EVENT_BUS.register(new DwClientHooks());
            MinecraftForge.EVENT_BUS.register(new InventoryButtonInjector());
            MinecraftForge.EVENT_BUS.register(new InventoryXpOverlay());
            MinecraftForge.EVENT_BUS.register(new AbilityBarOverlay());
        });
    }
}
