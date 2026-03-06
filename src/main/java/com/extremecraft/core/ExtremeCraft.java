package com.extremecraft.core;

import com.extremecraft.ability.AbilityEngine;
import com.extremecraft.ability.AbilityRegistry;
import com.extremecraft.api.ExtremeCraftAPI;
import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.client.DwClientHooks;
import com.extremecraft.client.ExtremeCraftKeybinds;
import com.extremecraft.client.DwKeybinds;`r`nimport com.extremecraft.client.input.ExtremeCraftKeybinds;
import com.extremecraft.client.gui.machine.TechMachineScreen;`r`nimport com.extremecraft.client.gui.hud.AbilityBarOverlay;
import com.extremecraft.client.gui.player.AbilityBarOverlay;
import com.extremecraft.client.gui.player.InventoryButtonInjector;
import com.extremecraft.client.gui.player.InventoryXpOverlay;
import com.extremecraft.combat.CombatEventHandler;
import com.extremecraft.combat.dualwield.PlayerDualWieldEvents;
import com.extremecraft.command.ECDevCommands;
import com.extremecraft.config.DwConfig;
import com.extremecraft.future.registry.TechBlockEntities;
import com.extremecraft.future.registry.TechBlocks;
import com.extremecraft.future.registry.TechCreativeTabs;
import com.extremecraft.future.registry.TechItems;
import com.extremecraft.future.registry.TechMenuTypes;
import com.extremecraft.future.registry.TechRecipeSerializers;
import com.extremecraft.gui.PulverizerScreen;
import com.extremecraft.item.armor.ArmorBonusHandler;
import com.extremecraft.machine.MachineRegistry;
import com.extremecraft.magic.SpellRegistry;
import com.extremecraft.magic.mana.ManaCapabilityEvents;
import com.extremecraft.modules.loader.ModuleAbilityLoader;
import com.extremecraft.modules.loader.ModuleDefinitionLoader;
import com.extremecraft.modules.runtime.ModuleRuntimeEvents;
import com.extremecraft.net.DwNetwork;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.sync.RuntimeSyncEvents;
import com.extremecraft.platform.CompatibilityGate;
import com.extremecraft.platform.ExtremeCraftApiProviderImpl;
import com.extremecraft.platform.data.loader.PlatformDataLoaderBootstrap;
import com.extremecraft.platform.data.sync.PlatformDataSyncEvents;
import com.extremecraft.platform.module.CoreGameplayModule;
import com.extremecraft.platform.module.ExtremeCraftModuleLoader;
import com.extremecraft.platform.module.ModuleRegistry;
import com.extremecraft.progression.ProgressCommands;
import com.extremecraft.progression.ProgressionEvents;
import com.extremecraft.progression.ProgressionRegistry;
import com.extremecraft.progression.StageDataLoader;
import com.extremecraft.progression.capability.PlayerStatsCapabilityEvents;
import com.extremecraft.progression.capability.PlayerStatsGameplayEvents;
import com.extremecraft.progression.capability.ProgressCapabilityEvents;`r`nimport com.extremecraft.progression.level.PlayerLevelCapabilityEvents;`r`nimport com.extremecraft.progression.level.PlayerLevelGameplayEvents;
import com.extremecraft.progression.classsystem.data.ClassAbilityLoader;
import com.extremecraft.progression.level.PlayerLevelEvents;
import com.extremecraft.progression.classsystem.data.ClassDefinitionLoader;
import com.extremecraft.progression.skilltree.PlayerSkillTreeEvents;
import com.extremecraft.progression.skilltree.SkillTreeDataLoader;
import com.extremecraft.progression.stage.StageCapabilityEvents;
import com.extremecraft.progression.unlock.UnlockRuleLoader;
import com.extremecraft.quest.QuestManager;
import com.extremecraft.registry.ModBlockEntities;
import com.extremecraft.registry.ModBlocks;
import com.extremecraft.registry.ModItems;
import com.extremecraft.registry.ModMenuTypes;
import com.extremecraft.registry.ModRecipeSerializers;
import com.extremecraft.research.ResearchCapabilityEvents;
import com.extremecraft.research.ResearchManager;
import com.extremecraft.server.DwServerTicker;
import com.extremecraft.skills.SkillRegistry;
import com.extremecraft.skills.SkillsCapabilityEvents;
import com.extremecraft.worldgen.DimensionHooks;
import com.extremecraft.worldgen.validation.WorldgenConsistencyValidator;
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
        DwConfig.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);

        TechBlocks.BLOCKS.register(modBus);
        TechItems.ITEMS.register(modBus);
        TechCreativeTabs.TABS.register(modBus);
        TechBlockEntities.BLOCK_ENTITIES.register(modBus);
        TechMenuTypes.MENUS.register(modBus);
        TechRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onEntityAttributeModification);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(DwKeybinds::onRegisterKeyMappings);`r`n            modBus.addListener(ExtremeCraftKeybinds::onRegisterKeyMappings);
            modBus.addListener(ExtremeCraftKeybinds::onRegisterKeyMappings);
        }

        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.init();
            DwNetwork.init();

            ExtremeCraftApiProviderImpl apiProvider = new ExtremeCraftApiProviderImpl();
            ExtremeCraftAPI.bootstrap(apiProvider);

            CoreGameplayModule coreModule = new CoreGameplayModule();
            if (CompatibilityGate.isCompatible(coreModule)) {
                coreModule.register(apiProvider);
                ModuleRegistry.register(coreModule);
            }
            ExtremeCraftModuleLoader.loadAll(apiProvider);

            MinecraftForge.EVENT_BUS.register(new ProgressCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerStatsCapabilityEvents());`r`n            MinecraftForge.EVENT_BUS.register(new PlayerLevelCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerStatsGameplayEvents());`r`n            MinecraftForge.EVENT_BUS.register(new PlayerLevelGameplayEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerLevelEvents());
            MinecraftForge.EVENT_BUS.register(new ManaCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new StageCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new SkillsCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new ResearchCapabilityEvents());

            MinecraftForge.EVENT_BUS.register(new ProgressionEvents());
            MinecraftForge.EVENT_BUS.register(new QuestManager());
            MinecraftForge.EVENT_BUS.register(new StageDataLoader());
            MinecraftForge.EVENT_BUS.register(new UnlockRuleLoader());
            MinecraftForge.EVENT_BUS.register(new SkillRegistry());
            MinecraftForge.EVENT_BUS.register(new SkillTreeDataLoader());
            MinecraftForge.EVENT_BUS.register(new ClassDefinitionLoader());
            MinecraftForge.EVENT_BUS.register(new ClassAbilityLoader());
            MinecraftForge.EVENT_BUS.register(new ModuleDefinitionLoader());
            MinecraftForge.EVENT_BUS.register(new ModuleAbilityLoader());
            MinecraftForge.EVENT_BUS.register(new ResearchManager());
            MinecraftForge.EVENT_BUS.register(new WorldgenConsistencyValidator());
            MinecraftForge.EVENT_BUS.register(new DimensionHooks());
            MinecraftForge.EVENT_BUS.register(new DwServerTicker());
            MinecraftForge.EVENT_BUS.register(new ArmorBonusHandler());
            MinecraftForge.EVENT_BUS.register(new CombatEventHandler());
            MinecraftForge.EVENT_BUS.register(new PlayerDualWieldEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerSkillTreeEvents());
            MinecraftForge.EVENT_BUS.register(new ModuleRuntimeEvents());
            MinecraftForge.EVENT_BUS.register(new RuntimeSyncEvents());

            MinecraftForge.EVENT_BUS.register(new AbilityRegistry());
            AbilityEngine.initialize();
            MinecraftForge.EVENT_BUS.register(new SpellRegistry());
            MinecraftForge.EVENT_BUS.register(new ClassRegistry());
            MinecraftForge.EVENT_BUS.register(new MachineRegistry());
            MinecraftForge.EVENT_BUS.register(new ProgressionRegistry());

            PlatformDataLoaderBootstrap.registerAll();
            MinecraftForge.EVENT_BUS.register(new PlatformDataSyncEvents());
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.PULVERIZER_MENU.get(), PulverizerScreen::new);
            MenuScreens.register(TechMenuTypes.TECH_MACHINE.get(), TechMachineScreen::new);
            MinecraftForge.EVENT_BUS.register(new DwClientHooks());
            MinecraftForge.EVENT_BUS.register(new InventoryButtonInjector());
            MinecraftForge.EVENT_BUS.register(new InventoryXpOverlay());`r`n            MinecraftForge.EVENT_BUS.register(new AbilityBarOverlay());
            MinecraftForge.EVENT_BUS.register(new AbilityBarOverlay());
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        ProgressCommands.register(event.getDispatcher());
        ECDevCommands.register(event.getDispatcher());
    }

    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Reserved for future entity framework attribute injections.
    }
}




