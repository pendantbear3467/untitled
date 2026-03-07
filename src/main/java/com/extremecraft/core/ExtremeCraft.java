package com.extremecraft.core;

import com.extremecraft.ability.AbilityEngine;
import com.extremecraft.ability.AbilityRegistry;
import com.extremecraft.api.ExtremeCraftAPI;
import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.client.DwClientHooks;
import com.extremecraft.client.DwKeybinds;
import com.extremecraft.client.gui.player.AbilityBarOverlay;
import com.extremecraft.client.gui.machine.TechMachineScreen;
import com.extremecraft.client.gui.player.InventoryButtonInjector;
import com.extremecraft.client.gui.player.InventoryXpOverlay;
import com.extremecraft.client.ExtremeCraftKeybinds;
import com.extremecraft.client.render.entity.ModEntityRenderers;
import com.extremecraft.combat.CombatEventHandler;
import com.extremecraft.combat.dualwield.PlayerDualWieldEvents;
import com.extremecraft.command.ECDevCommands;
import com.extremecraft.config.Config;
import com.extremecraft.config.DwConfig;
import com.extremecraft.entity.ModEntities;
import com.extremecraft.entity.MobAttributes;
import com.extremecraft.entity.MobSpawns;
import com.extremecraft.entity.extension.EntityExtensionEvents;
import com.extremecraft.entity.system.BossArenaManager;
import com.extremecraft.entity.system.GameplayMechanicsEvents;
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
import com.extremecraft.platform.OptionalCompatHooks;
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
import com.extremecraft.progression.capability.ProgressCapabilityEvents;
import com.extremecraft.progression.classsystem.data.ClassAbilityLoader;
import com.extremecraft.progression.classsystem.data.ClassDefinitionLoader;
import com.extremecraft.progression.level.PlayerLevelCapabilityEvents;
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
import com.extremecraft.registry.ModSounds;
import com.extremecraft.research.ResearchCapabilityEvents;
import com.extremecraft.research.ResearchManager;
import com.extremecraft.server.DwServerTicker;
import com.extremecraft.server.PlayerRuntimeCleanupEvents;
import com.extremecraft.server.task.ServerDeferredWorkEvents;
import com.extremecraft.skills.SkillRegistry;
import com.extremecraft.skills.SkillsCapabilityEvents;
import com.extremecraft.worldgen.DimensionHooks;
import com.extremecraft.worldgen.validation.WorldgenConsistencyValidator;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Primary Forge mod bootstrap for ExtremeCraft.
 *
 * <p>This class wires gameplay subsystems into Forge lifecycle phases and event buses.
 * Keep orchestration logic here and place domain behavior in dedicated system classes
 * so startup ordering remains explicit and reviewable.</p>
 */
@Mod(ECConstants.MODID)
public final class ExtremeCraft {
    public ExtremeCraft() {
        Config.register();
        DwConfig.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModSounds.SOUND_EVENTS.register(modBus);

        TechBlocks.BLOCKS.register(modBus);
        TechItems.ITEMS.register(modBus);
        TechCreativeTabs.TABS.register(modBus);
        TechBlockEntities.BLOCK_ENTITIES.register(modBus);
        TechMenuTypes.MENUS.register(modBus);
        TechRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModEntities.ENTITY_TYPES.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::onEntityAttributeModification);
        modBus.addListener(this::onEntityAttributeCreation);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(DwKeybinds::onRegisterKeyMappings);
            modBus.addListener(ExtremeCraftKeybinds::onRegisterKeyMappings);
            modBus.addListener(ModEntityRenderers::registerRenderers);
            modBus.addListener(ModEntityRenderers::registerLayerDefinitions);
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
            OptionalCompatHooks.bootstrap();

            MinecraftForge.EVENT_BUS.register(new ProgressCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerStatsCapabilityEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerStatsGameplayEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerLevelCapabilityEvents());
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
            // Tick-scheduled server systems: offhand break replay, deferred queue draining, and stale state cleanup.

            MinecraftForge.EVENT_BUS.register(new DwServerTicker());
            MinecraftForge.EVENT_BUS.register(new PlayerRuntimeCleanupEvents());
            MinecraftForge.EVENT_BUS.register(new ServerDeferredWorkEvents());
            MinecraftForge.EVENT_BUS.register(new ArmorBonusHandler());
            MinecraftForge.EVENT_BUS.register(new CombatEventHandler());
            MinecraftForge.EVENT_BUS.register(new PlayerDualWieldEvents());
            MinecraftForge.EVENT_BUS.register(new PlayerSkillTreeEvents());
            MinecraftForge.EVENT_BUS.register(new ModuleRuntimeEvents());
            MinecraftForge.EVENT_BUS.register(new RuntimeSyncEvents());
            MinecraftForge.EVENT_BUS.register(new GameplayMechanicsEvents());
            MinecraftForge.EVENT_BUS.register(new BossArenaManager());
            MinecraftForge.EVENT_BUS.register(new EntityExtensionEvents());

            MinecraftForge.EVENT_BUS.register(new AbilityRegistry());
            AbilityEngine.initialize();
            MinecraftForge.EVENT_BUS.register(new SpellRegistry());
            MinecraftForge.EVENT_BUS.register(new ClassRegistry());
            MinecraftForge.EVENT_BUS.register(new MachineRegistry());
            MinecraftForge.EVENT_BUS.register(new ProgressionRegistry());

            MobSpawns.bootstrap();
            // Register datapack reload listeners after gameplay registries are initialized.

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
            MinecraftForge.EVENT_BUS.register(new InventoryXpOverlay());
            MinecraftForge.EVENT_BUS.register(new AbilityBarOverlay());
        });
    }

    private void registerCommands(RegisterCommandsEvent event) {
        ProgressCommands.register(event.getDispatcher());
        ECDevCommands.register(event.getDispatcher());
    }


    private void onEntityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TECH_CONSTRUCT.get(), MobAttributes.basic(38.0F, 7.0F, 0.25F, 6.0F).build());
        event.put(ModEntities.ARCANE_WRAITH.get(), MobAttributes.basic(26.0F, 6.0F, 0.30F, 2.0F).build());
        event.put(ModEntities.VOID_STALKER.get(), MobAttributes.basic(34.0F, 8.0F, 0.34F, 4.0F).build());
        event.put(ModEntities.ANCIENT_SENTINEL.get(), MobAttributes.basic(46.0F, 9.0F, 0.22F, 8.0F).build());
        event.put(ModEntities.ENERGY_PARASITE.get(), MobAttributes.basic(20.0F, 5.5F, 0.36F, 1.0F).build());
        event.put(ModEntities.RUNIC_GOLEM.get(), MobAttributes.basic(64.0F, 11.0F, 0.20F, 10.0F).build());

        event.put(ModEntities.ANCIENT_CORE_GUARDIAN.get(), MobAttributes.boss(260.0F, 15.0F, 0.24F, 14.0F, 3.0F).build());
        event.put(ModEntities.VOID_TITAN.get(), MobAttributes.boss(340.0F, 18.0F, 0.23F, 16.0F, 4.0F).build());
        event.put(ModEntities.OVERCHARGED_MACHINE_GOD.get(), MobAttributes.boss(420.0F, 21.0F, 0.27F, 18.0F, 4.0F).build());
    }

    private void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        // Reserved for future entity framework attribute injections.
    }
}



















