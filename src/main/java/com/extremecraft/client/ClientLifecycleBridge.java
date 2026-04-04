package com.extremecraft.client;

import com.extremecraft.client.gui.machine.GeneratorMachineScreen;
import com.extremecraft.client.gui.machine.MagicMachineScreen;
import com.extremecraft.client.gui.machine.ReactorMachineScreen;
import com.extremecraft.client.gui.machine.TechMachineScreen;
import com.extremecraft.client.gui.player.AbilityBarOverlay;
import com.extremecraft.client.gui.player.InventoryButtonInjector;
import com.extremecraft.client.gui.player.InventoryXpOverlay;
import com.extremecraft.client.render.entity.ModEntityRenderers;
import com.extremecraft.future.registry.TechMenuTypes;
import com.extremecraft.gui.PulverizerScreen;
import com.extremecraft.machine.core.MachineCatalog;
import com.extremecraft.machine.core.MachineCategory;
import com.extremecraft.machine.menu.TechMachineMenu;
import com.extremecraft.reactor.ReactorIdentity;
import com.extremecraft.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-only lifecycle wiring isolated from common bootstrap.
 */
public final class ClientLifecycleBridge {
    private static final Logger LOGGER = LogManager.getLogger();

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
            MenuScreens.register(TechMenuTypes.TECH_MACHINE.get(), ClientLifecycleBridge::createTechMachineScreen);
            MinecraftForge.EVENT_BUS.register(new DwClientHooks());
            MinecraftForge.EVENT_BUS.register(new ClientRuntimeCleanupEvents());
            MinecraftForge.EVENT_BUS.register(new InventoryButtonInjector());
            MinecraftForge.EVENT_BUS.register(new InventoryXpOverlay());
            MinecraftForge.EVENT_BUS.register(new AbilityBarOverlay());
        });
    }

    private static TechMachineScreen createTechMachineScreen(TechMachineMenu menu, Inventory inventory, Component title) {
        String machineId = menu.machineId();
        if (ReactorIdentity.isFirstReleaseReactor(machineId)) {
            return new ReactorMachineScreen(menu, inventory, title);
        }

        MachineCategory category = MachineCatalog.byId(machineId)
                .map(definition -> definition.category())
                .orElseGet(() -> {
                    LOGGER.warn("[ClientUI] Unknown machine id '{}' defaulted to PROCESSOR screen. Add it to MachineCatalog to keep UI family routing aligned with backend behavior.", machineId);
                    return MachineCategory.PROCESSOR;
                });

        return switch (category) {
            case GENERATOR -> new GeneratorMachineScreen(menu, inventory, title);
            case MAGIC -> new MagicMachineScreen(menu, inventory, title);
            default -> new TechMachineScreen(menu, inventory, title);
        };
    }
}
