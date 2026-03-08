package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.quest.QuestDefinition;
import com.extremecraft.quest.QuestManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ProgressCommands {
    private ProgressCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("extremecraft")
                        .then(Commands.literal("level")
                                .then(Commands.literal("get").executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    return ProgressApi.get(player).map(data -> {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Level " + data.level() + " XP " + data.xp() + "/" + PlayerProgressData.xpToNextLevel(data.level())), false);
                                        return 1;
                                    }).orElse(0);
                                }))
                                .then(Commands.literal("set")
                                        .requires(src -> src.hasPermission(2))
                                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 999)).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                            ProgressionMutationService.setLevel(player, value);
                                            return 1;
                                        }))))
                        .then(Commands.literal("class")
                                .then(Commands.literal("list").executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    return ProgressApi.get(player).map(data -> {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Unlocked: " + String.join(", ", data.unlockedClasses())), false);
                                        ctx.getSource().sendSuccess(() -> Component.literal("Current class mastery: " + data.currentClass() + " L" + data.getClassLevel(data.currentClass()) + " XP " + data.getClassExperience(data.currentClass()) + "/" + PlayerProgressData.classXpForNextLevel(data.getClassLevel(data.currentClass()))), false);
                                        return 1;
                                    }).orElse(0);
                                }))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            if (ProgressionService.switchClass(player, id)) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("Class switched to " + id), false);
                                                return 1;
                                            }
                                            ctx.getSource().sendFailure(Component.literal("Class not unlocked: " + id));
                                            return 0;
                                        }))))
                        .then(Commands.literal("quest")
                                .then(Commands.literal("list").executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    return ProgressApi.get(player).map(data -> {
                                        for (QuestDefinition quest : QuestManager.all()) {
                                            int progress = data.getQuestProgress(quest.id());
                                            boolean done = data.isQuestCompleted(quest.id());
                                            String status = done ? "CLAIMED" : (progress >= quest.target() ? "READY" : "IN_PROGRESS");
                                            ctx.getSource().sendSuccess(() -> Component.literal(quest.id() + " [" + status + "] " + progress + "/" + quest.target()), false);
                                        }
                                        return 1;
                                    }).orElse(0);
                                }))
                                .then(Commands.literal("claim")
                                        .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            String id = StringArgumentType.getString(ctx, "id");
                                            QuestDefinition quest = QuestManager.byId(id);
                                            if (quest == null) {
                                                ctx.getSource().sendFailure(Component.literal("Unknown quest: " + id));
                                                return 0;
                                            }

                                            if (!ProgressionFacade.claimGuildQuestReward(player, quest)) {
                                                return ProgressApi.get(player).map(data -> {
                                                    if (data.isQuestCompleted(id)) {
                                                        ctx.getSource().sendFailure(Component.literal("Quest already claimed."));
                                                        return 0;
                                                    }

                                                    int progress = data.getQuestProgress(id);
                                                    if (progress < quest.target()) {
                                                        ctx.getSource().sendFailure(Component.literal("Quest incomplete: " + progress + "/" + quest.target()));
                                                    }
                                                    return 0;
                                                }).orElse(0);
                                            }

                                            ctx.getSource().sendSuccess(() -> Component.literal("Quest claimed: " + quest.title()), false);
                                            return 1;
                                        }))))
        );
    }
}
