package com.extremecraft.core;

import com.extremecraft.client.DwClientHooks;
import com.extremecraft.client.DwKeybinds;
import com.extremecraft.gui.PulverizerScreen;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.progression.ProgressCommands;
import com.extremecraft.progression.ProgressionEvents;
import com.extremecraft.progression.capability.ProgressCapabilityEvents;
import com.extremecraft.quest.QuestManager;
import com.extremecraft.registry.ModBlockEntities;
import com.extremecraft.registry.ModBlocks;
import com.extremecraft.registry.ModItems;
import com.extremecraft.registry.ModMenuTypes;
import com.extremecraft.registry.ModRecipeSerializers;
import com.extremecraft.server.DwServerTicker;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ECConstants.MODID)
public final class ExtremeCraft {
    public ExtremeCraft() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onEntityAttributeModification);

        MinecraftForge.EVENT_BUS.register(new ProgressCapabilityEvents());
        MinecraftForge.EVENT_BUS.register(new ProgressionEvents());
        MinecraftForge.EVENT_BUS.register(new QuestManager());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);

        MinecraftForge.EVENT_BUS.register(new DwServerTicker());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(DwKeybinds::onRegisterKeyMappings);
            MinecraftForge.EVENT_BUS.register(new DwClientHooks());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::init);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ModMenuTypes.PULVERIZER_MENU.get(), PulverizerScreen::new));
    }

    private void registerCommands(RegisterCommandsEvent event) {
        ProgressCommands.register(event.getDispatcher());
    }

    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Reserved for future entity framework attribute injections.
    }
}
