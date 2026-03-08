package com.extremecraft.progression;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.stage.ProgressionStage;
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
        dispatcher.register(Commands.literal("extremecraft")
                .then(Commands.literal("level")
                        .then(Commands.literal("get").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return ProgressApi.get(p).map(data -> {
                                ctx.getSource().sendSuccess(() -> Component.literal("Level " + data.level() + " XP " + data.xp() + "/" + PlayerProgressData.xpToNextLevel(data.level())), false);
                                return 1;
                            }).orElse(0);
                        }))
                        .then(Commands.literal("set")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 999)).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int value = IntegerArgumentType.getInteger(ctx, "value");
                                    ProgressionMutationService.setLevel(p, value);
                                    return 1;
                                })))))
                .then(Commands.literal("class")
                        .then(Commands.literal("list").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return ProgressApi.get(p).map(data -> {
                                ctx.getSource().sendSuccess(() -> Component.literal("Unlocked: " + String.join(", ", data.unlockedClasses())), false);
                                ctx.getSource().sendSuccess(() -> Component.literal("Current class mastery: " + data.currentClass() + " L" + data.getClassLevel(data.currentClass()) + " XP " + data.getClassExperience(data.currentClass()) + "/" + PlayerProgressData.classXpForNextLevel(data.getClassLevel(data.currentClass()))), false);
                                return 1;
                            }).orElse(0);
                        }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    if (ProgressionService.switchClass(p, id)) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("Class switched to " + id), false);
                                        return 1;
                                    }
                                    ctx.getSource().sendFailure(Component.literal("Class not unlocked: " + id));
                                    return 0;
                                })))))
                .then(Commands.literal("quest")
                        .then(Commands.literal("list").executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            return ProgressApi.get(p).map(data -> {
                                for (QuestDefinition q : QuestManager.all()) {
                                    int prog = data.getQuestProgress(q.id());
                                    boolean done = data.isQuestCompleted(q.id());
                                    String status = done ? "CLAIMED" : (prog >= q.target() ? "READY" : "IN_PROGRESS");
                                    ctx.getSource().sendSuccess(() -> Component.literal(q.id() + " [" + status + "] " + prog + "/" + q.target()), false);
                                }
                                return 1;
                            }).orElse(0);
                        }))
                        .then(Commands.literal("claim")
                                .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    QuestDefinition q = QuestManager.byId(id);
                                    if (q == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown quest: " + id));
                                        return 0;
                                    }

                                    if (!ProgressionFacade.claimGuildQuestReward(p, q)) {
                                        return ProgressApi.get(p).map(data -> {
                                            if (data.isQuestCompleted(id)) {
                                                ctx.getSource().sendFailure(Component.literal("Quest already claimed."));
                                                return 0;
                                            }

                                            int prog = data.getQuestProgress(id);
                                            if (prog < q.target()) {
                                                ctx.getSource().sendFailure(Component.literal("Quest incomplete: " + prog + "/" + q.target()));
                                                return 0;
                                            }
                                            return 0;
                                        }).orElse(0);
                                    }

                                    ctx.getSource().sendSuccess(() -> Component.literal("Quest claimed: " + q.title()), false);
                                    return 1;
                                })))))
        );
    }
}
