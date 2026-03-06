package com.extremecraft.command;

import com.extremecraft.api.ExtremeCraftAPI;
import com.extremecraft.platform.module.ModuleRegistry;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

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
                .then(Commands.literal("reload")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            server.reloadResources(server.getPackRepository().getSelectedIds())
                                    .whenComplete((ignored, error) -> {
                                        if (error != null) {
                                            ctx.getSource().sendFailure(Component.literal("Reload failed: " + error.getMessage()));
                                            return;
                                        }
                                        ctx.getSource().sendSuccess(() -> Component.literal("Datapacks reloaded."), true);
                                    });
                            ctx.getSource().sendSuccess(() -> Component.literal("Reload started..."), false);
                            return 1;
                        })));
    }
}
