package com.extremecraft.command;

import com.extremecraft.ability.AbilityExecutor;
import com.extremecraft.api.ExtremeCraftAPI;
import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.magic.SpellCastingSystem;
import com.extremecraft.magic.mana.ManaService;
import com.extremecraft.machine.MachineRegistry;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.OpenExtremeCraftDebugScreenS2CPacket;
import com.extremecraft.network.sync.RuntimeSyncService;
import com.extremecraft.platform.data.registry.AbilityDataRegistry;
import com.extremecraft.platform.data.registry.ClassDataRegistry;
import com.extremecraft.platform.data.registry.DimensionDataRegistry;
import com.extremecraft.platform.data.registry.ItemDataRegistry;
import com.extremecraft.platform.data.registry.LootTableDataRegistry;
import com.extremecraft.platform.data.registry.MachineDataRegistry;
import com.extremecraft.platform.data.registry.MaterialDataRegistry;
import com.extremecraft.platform.data.registry.ModuleDataRegistry;
import com.extremecraft.platform.data.registry.QuestDataRegistry;
import com.extremecraft.platform.data.registry.RecipeDataRegistry;
import com.extremecraft.platform.data.registry.RegistryDumpService;
import com.extremecraft.platform.data.registry.ResearchDataRegistry;
import com.extremecraft.platform.data.registry.SkillTreeDataRegistry;
import com.extremecraft.platform.data.registry.StructureDataRegistry;
import com.extremecraft.platform.data.registry.TechTreeDataRegistry;
import com.extremecraft.platform.data.registry.UpgradeDataRegistry;
import com.extremecraft.platform.data.registry.WorldGenerationDataRegistry;
import com.extremecraft.platform.data.validator.PlatformValidationRunner;
import com.extremecraft.platform.module.ModuleRegistry;
import com.extremecraft.progression.ProgressionService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.stream.Collectors;

public final class ECDevCommands {
    private ECDevCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ec")
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal("ExtremeCraft Debug"), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Loaded modules: " + ModuleRegistry.size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("API machines: " + ExtremeCraftAPI.machines().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("API skills: " + ExtremeCraftAPI.skillTrees().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("API quests: " + ExtremeCraftAPI.quests().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("API spells: " + ExtremeCraftAPI.spells().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("API classes: " + ExtremeCraftAPI.classes().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Datapack machines: " + MachineDataRegistry.registry().size()), false);
                            ctx.getSource().sendSuccess(() -> Component.literal("Datapack tech trees: " + TechTreeDataRegistry.registry().size()), false);
                            return 1;
                        }))
                .then(Commands.literal("validate")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            int warnings = PlatformValidationRunner.validateAll();
                            ctx.getSource().sendSuccess(() -> Component.literal("Validation completed. warnings=" + warnings), true);
                            return warnings == 0 ? 1 : 0;
                        }))
                .then(Commands.literal("dump_registry")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("materials", MaterialDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("machines", MachineDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("tech_trees", TechTreeDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("skill_trees", SkillTreeDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("quests", QuestDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("structures", StructureDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("dimensions", DimensionDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("modules", ModuleDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("classes", ClassDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("abilities", AbilityDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("items", ItemDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("recipes", RecipeDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("loot", LootTableDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("upgrades", UpgradeDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("research", ResearchDataRegistry.registry())), false);
                            ctx.getSource().sendSuccess(() -> Component.literal(RegistryDumpService.summary("world_generation", WorldGenerationDataRegistry.registry())), false);
                            return 1;
                        }))
                .then(Commands.literal("modules")
                        .executes(ctx -> {
                            String modules = ModuleRegistry.all().stream()
                                    .map(m -> m.moduleId() + "@api" + m.apiVersion())
                                    .collect(Collectors.joining(", "));
                            if (modules.isBlank()) {
                                modules = "<none>";
                            }
                            String finalModules = modules;
                            ctx.getSource().sendSuccess(() -> Component.literal("Modules: " + finalModules), false);
                            return 1;
                        }))
                .then(Commands.literal("ability")
                        .then(Commands.literal("test")
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String abilityId = StringArgumentType.getString(ctx, "ability");
                                            boolean success = AbilityExecutor.tryActivate(player, abilityId);
                                            RuntimeSyncService.syncAbilities(player);
                                            if (!success) {
                                                ctx.getSource().sendFailure(Component.literal("Ability failed: " + abilityId));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Ability executed: " + abilityId), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("spell")
                        .then(Commands.literal("cast")
                                .then(Commands.argument("spell", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String spellId = StringArgumentType.getString(ctx, "spell");
                                            boolean success = SpellCastingSystem.tryCast(player, spellId);
                                            RuntimeSyncService.syncAbilities(player);
                                            if (!success) {
                                                ctx.getSource().sendFailure(Component.literal("Spell failed: " + spellId));
                                                return 0;
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Spell cast: " + spellId), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("mana")
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, 100000))
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            ManaService.setMana(player, value);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Mana set to " + value), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("class")
                        .then(Commands.literal("set")
                                .then(Commands.argument("class", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String classId = StringArgumentType.getString(ctx, "class").trim().toLowerCase();
                                            if (ClassRegistry.get(classId) == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown class: " + classId));
                                                return 0;
                                            }

                                            if (!ProgressionService.switchClass(player, classId)) {
                                                ctx.getSource().sendFailure(Component.literal("Class not unlocked: " + classId));
                                                return 0;
                                            }

                                            RuntimeSyncService.syncAll(player);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Class set to " + classId), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("machine")
                        .then(Commands.literal("debug")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    RuntimeSyncService.syncMachineStates(player);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Runtime machines=" + MachineRegistry.machines().size() +
                                            ", runtime recipes=" + MachineRegistry.recipes().size()), false);
                                    return 1;
                                })))
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reloadData(ctx.getSource())))
                .then(Commands.literal("reload_data")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> reloadData(ctx.getSource())))
                .then(Commands.literal("debug_screen")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenExtremeCraftDebugScreenS2CPacket());
                            return 1;
                        }))
        );
    }

    private static int reloadData(CommandSourceStack source) {
        var server = source.getServer();
        server.reloadResources(server.getPackRepository().getSelectedIds())
                .whenComplete((ignored, error) -> {
                    if (error != null) {
                        source.sendFailure(Component.literal("Reload failed: " + error.getMessage()));
                        return;
                    }
                    source.sendSuccess(() -> Component.literal("Datapacks reloaded."), true);
                });
        source.sendSuccess(() -> Component.literal("Reload started..."), false);
        return 1;
    }
}
