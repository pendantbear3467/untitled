package com.extremecraft.game;

import com.extremecraft.machines.MachineBlueprints;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ProgressionSystem {
    private static final String TAG_LEVEL = "ec_level";
    private static final String TAG_XP = "ec_xp";
    private static final String TAG_PLAYER_SKILL_POINTS = "ec_player_skill_points";
    private static final String TAG_CLASS_SKILL_POINTS = "ec_class_skill_points";
    private static final String TAG_CLASS = "ec_class";
    private static final String TAG_UNLOCKED_CLASSES = "ec_unlocked_classes";
    private static final String TAG_MOB_SCALED = "ec_mob_scaled";

    private static final String TAG_QUEST_PROGRESS_PREFIX = "ec_qp_";
    private static final String TAG_QUEST_DONE_PREFIX = "ec_qd_";

    private static final UUID LEVEL_HEALTH_MOD = UUID.fromString("e1c00b0a-4dbb-4f41-a5b1-c39de1f96d22");
    private static final UUID LEVEL_ATTACK_MOD = UUID.fromString("cbd1aa03-fd31-4ef0-90ea-80f2306b9521");
    private static final UUID CLASS_HEALTH_MOD = UUID.fromString("16af727e-47fd-40fb-9259-d8e37d08ef1a");
    private static final UUID CLASS_ATTACK_MOD = UUID.fromString("ac3bf31c-fdb4-47a4-90fa-cd514f9ef7f8");
    private static final UUID CLASS_SPEED_MOD = UUID.fromString("186fa38f-ffb4-41e9-8919-1d99bb604fdf");
    private static final UUID CLASS_LUCK_MOD = UUID.fromString("4a9f39fd-4f5b-4dc8-80a0-d93e54ef5c2b");

    @SubscribeEvent
    public void onMobKilled(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (event.getEntity() instanceof ServerPlayer) return;

        LivingEntity target = event.getEntity();
        int gained = Math.max(5, (int) (target.getMaxHealth() * 2.0));
        addXp(player, gained);
        addQuestProgress(player, QuestType.KILL_MOBS, 1);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        addQuestProgress(player, QuestType.BREAK_BLOCKS, 1);
    }

    @SubscribeEvent
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        addQuestProgress(player, QuestType.CRAFT_ITEMS, event.getCrafting().getCount());
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        CompoundTag oldData = event.getOriginal().getPersistentData();
        CompoundTag newData = newPlayer.getPersistentData();

        for (String key : oldData.getAllKeys()) {
            if (key.startsWith("ec_")) {
                newData.put(key, oldData.get(key).copy());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ensureDefaults(player);
        applyPlayerModifiers(player);
    }

    @SubscribeEvent
    public void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (mob.level().isClientSide) return;

        CompoundTag tag = mob.getPersistentData();
        if (tag.getBoolean(TAG_MOB_SCALED)) return;

        var nearestAny = mob.level().getNearestPlayer(mob, 64.0);
        if (!(nearestAny instanceof ServerPlayer nearest)) return;

        ensureDefaults(nearest);
        int level = getLevel(nearest);
        double damage = nearest.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double health = nearest.getMaxHealth();

        double playerPower = (level * 1.35) + (damage * 0.90) + (health * 0.15);
        double scale = Mth.clamp(0.90 + (playerPower / 24.0), 0.85, 4.00);

        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * scale);
            mob.setHealth((float) maxHealth.getValue());
        }

        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double attackScale = Mth.clamp(0.75 + (scale * 0.70), 0.70, 3.50);
            attack.setBaseValue(attack.getBaseValue() * attackScale);
        }

        tag.putBoolean(TAG_MOB_SCALED, true);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("extremecraft");

        root.then(Commands.literal("level")
                .then(Commands.literal("get").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    ensureDefaults(p);
                    ctx.getSource().sendSuccess(() -> Component.literal("Level: " + getLevel(p) + " | XP: " + getXp(p) + "/" + xpToNext(getLevel(p))), false);
                    return 1;
                }))
                .then(Commands.literal("set")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 999))
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int v = IntegerArgumentType.getInteger(ctx, "value");
                                    setLevel(p, v);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Set level to " + v), true);
                                    return 1;
                                }))));

        root.then(Commands.literal("xp")
                .then(Commands.literal("add")
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100000))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    int amt = IntegerArgumentType.getInteger(ctx, "amount");
                                    addXp(p, amt);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Added " + amt + " XP."), false);
                                    return 1;
                                }))));

        root.then(Commands.literal("class")
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(ctx -> setClassCommand(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"), false, ctx.getSource()))))
                .then(Commands.literal("unlock")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> setClassCommand(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "name"), true, ctx.getSource()))))
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ctx.getSource().sendSuccess(() -> Component.literal("Unlocked classes: " + String.join(", ", getUnlockedClasses(p))), false);
                            return 1;
                        })));

        root.then(Commands.literal("skills")
                .then(Commands.literal("get")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            ensureDefaults(p);
                            int playerPts = p.getPersistentData().getInt(TAG_PLAYER_SKILL_POINTS);
                            int classPts = p.getPersistentData().getInt(TAG_CLASS_SKILL_POINTS);
                            ctx.getSource().sendSuccess(() -> Component.literal("Player Skill Points: " + playerPts + " | Class Skill Points: " + classPts), false);
                            return 1;
                        })));

        root.then(Commands.literal("quest")
                .then(Commands.literal("list")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            QuestRegistry.all().values().forEach(q -> {
                                int prog = getQuestProgress(p, q.id());
                                boolean done = isQuestClaimed(p, q.id());
                                String status = done ? "[CLAIMED]" : (prog >= q.target() ? "[READY]" : "[IN PROGRESS]");
                                ctx.getSource().sendSuccess(() -> Component.literal(q.id() + " " + status + " " + prog + "/" + q.target() + " -> unlock " + q.rewardClassUnlock()), false);
                            });
                            return 1;
                        }))
                .then(Commands.literal("status")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String id = StringArgumentType.getString(ctx, "id");
                                    QuestDefinition q = QuestRegistry.get(id);
                                    if (q == null) {
                                        ctx.getSource().sendFailure(Component.literal("Unknown quest: " + id));
                                        return 0;
                                    }
                                    int prog = getQuestProgress(p, q.id());
                                    boolean done = isQuestClaimed(p, q.id());
                                    ctx.getSource().sendSuccess(() -> Component.literal(q.title() + " | " + prog + "/" + q.target() + " | claimed=" + done), false);
                                    return 1;
                                })))
                .then(Commands.literal("claim")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> claimQuest(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"), ctx.getSource())))));

        root.then(Commands.literal("machines")
                .then(Commands.literal("processing").executes(ctx -> sendList(ctx.getSource(), "Processing", MachineBlueprints.ORE_PROCESSING_LINE)))
                .then(Commands.literal("farmer").executes(ctx -> sendList(ctx.getSource(), "Auto Farmer", MachineBlueprints.FARM_AUTOMATION_LINE)))
                .then(Commands.literal("wind").executes(ctx -> sendList(ctx.getSource(), "Wind", MachineBlueprints.WINDMILL_LINE)))
                .then(Commands.literal("generators").executes(ctx -> sendList(ctx.getSource(), "Generator Tiers", MachineBlueprints.GENERATOR_TIERS)))
                .then(Commands.literal("reactor").executes(ctx -> sendList(ctx.getSource(), "Reactor", MachineBlueprints.REACTOR_LINE))));

        event.getDispatcher().register(root);
    }

    private static int sendList(CommandSourceStack source, String title, java.util.List<String> rows) {
        source.sendSuccess(() -> Component.literal("== " + title + " =="), false);
        rows.forEach(s -> source.sendSuccess(() -> Component.literal(" - " + s), false));
        return 1;
    }

    private static int setClassCommand(ServerPlayer p, String id, boolean forceUnlock, CommandSourceStack source) {
        PlayerClass klass = parseClass(id);
        if (klass == null) {
            source.sendFailure(Component.literal("Unknown class: " + id));
            return 0;
        }
        if (forceUnlock) unlockClass(p, klass.id);

        if (!getUnlockedClasses(p).contains(klass.id)) {
            source.sendFailure(Component.literal("Class is locked. Complete quests or use admin unlock."));
            return 0;
        }

        p.getPersistentData().putString(TAG_CLASS, klass.id);
        source.sendSuccess(() -> Component.literal("Class set to " + klass.id), false);
        applyPlayerModifiers(p);
        return 1;
    }

    private static int claimQuest(ServerPlayer p, String id, CommandSourceStack source) {
        QuestDefinition q = QuestRegistry.get(id);
        if (q == null) {
            source.sendFailure(Component.literal("Unknown quest: " + id));
            return 0;
        }
        if (isQuestClaimed(p, id)) {
            source.sendFailure(Component.literal("Quest already claimed."));
            return 0;
        }
        int progress = getQuestProgress(p, id);
        if (progress < q.target()) {
            source.sendFailure(Component.literal("Quest not complete: " + progress + "/" + q.target()));
            return 0;
        }

        setQuestClaimed(p, id);
        unlockClass(p, q.rewardClassUnlock());
        addSkillPoints(p, q.rewardPlayerSkillPoints(), q.rewardClassSkillPoints());
        source.sendSuccess(() -> Component.literal("Quest claimed: " + q.title() + " | unlocked " + q.rewardClassUnlock()), false);
        return 1;
    }

    private static void addQuestProgress(ServerPlayer player, QuestType type, int amount) {
        ensureDefaults(player);
        if (amount <= 0) return;

        for (QuestDefinition q : QuestRegistry.all().values()) {
            if (q.type() != type || isQuestClaimed(player, q.id())) continue;

            int cur = getQuestProgress(player, q.id());
            int next = Math.min(q.target(), cur + amount);
            player.getPersistentData().putInt(TAG_QUEST_PROGRESS_PREFIX + q.id(), next);
        }
    }

    private static int getQuestProgress(ServerPlayer player, String questId) {
        return player.getPersistentData().getInt(TAG_QUEST_PROGRESS_PREFIX + questId);
    }

    private static boolean isQuestClaimed(ServerPlayer player, String questId) {
        return player.getPersistentData().getBoolean(TAG_QUEST_DONE_PREFIX + questId);
    }

    private static void setQuestClaimed(ServerPlayer player, String questId) {
        player.getPersistentData().putBoolean(TAG_QUEST_DONE_PREFIX + questId, true);
    }

    private static void ensureDefaults(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(TAG_LEVEL)) data.putInt(TAG_LEVEL, 1);
        if (!data.contains(TAG_XP)) data.putInt(TAG_XP, 0);
        if (!data.contains(TAG_PLAYER_SKILL_POINTS)) data.putInt(TAG_PLAYER_SKILL_POINTS, 0);
        if (!data.contains(TAG_CLASS_SKILL_POINTS)) data.putInt(TAG_CLASS_SKILL_POINTS, 0);
        if (!data.contains(TAG_CLASS)) data.putString(TAG_CLASS, PlayerClass.WARRIOR.id);
        if (!data.contains(TAG_UNLOCKED_CLASSES)) data.putString(TAG_UNLOCKED_CLASSES, PlayerClass.WARRIOR.id);
    }

    private static void addXp(ServerPlayer player, int amount) {
        ensureDefaults(player);
        CompoundTag data = player.getPersistentData();
        int level = data.getInt(TAG_LEVEL);
        int xp = data.getInt(TAG_XP) + Math.max(0, amount);

        while (xp >= xpToNext(level)) {
            xp -= xpToNext(level);
            level++;
            data.putInt(TAG_PLAYER_SKILL_POINTS, data.getInt(TAG_PLAYER_SKILL_POINTS) + 1);
            if (level % 3 == 0) data.putInt(TAG_CLASS_SKILL_POINTS, data.getInt(TAG_CLASS_SKILL_POINTS) + 1);
        }

        data.putInt(TAG_LEVEL, level);
        data.putInt(TAG_XP, xp);
        applyPlayerModifiers(player);
    }

    private static void addSkillPoints(ServerPlayer player, int playerPoints, int classPoints) {
        ensureDefaults(player);
        CompoundTag data = player.getPersistentData();
        data.putInt(TAG_PLAYER_SKILL_POINTS, data.getInt(TAG_PLAYER_SKILL_POINTS) + Math.max(0, playerPoints));
        data.putInt(TAG_CLASS_SKILL_POINTS, data.getInt(TAG_CLASS_SKILL_POINTS) + Math.max(0, classPoints));
    }

    private static void setLevel(ServerPlayer player, int level) {
        ensureDefaults(player);
        CompoundTag data = player.getPersistentData();
        data.putInt(TAG_LEVEL, Mth.clamp(level, 1, 999));
        data.putInt(TAG_XP, 0);
        applyPlayerModifiers(player);
    }

    private static int getLevel(ServerPlayer player) {
        ensureDefaults(player);
        return player.getPersistentData().getInt(TAG_LEVEL);
    }

    private static int getXp(ServerPlayer player) {
        ensureDefaults(player);
        return player.getPersistentData().getInt(TAG_XP);
    }

    private static int xpToNext(int level) {
        return 50 + (level * level * 12);
    }

    private static PlayerClass getClass(ServerPlayer player) {
        ensureDefaults(player);
        return PlayerClass.fromId(player.getPersistentData().getString(TAG_CLASS));
    }

    private static PlayerClass parseClass(String id) {
        if (id == null) return null;
        String normalized = id.toLowerCase();
        return Arrays.stream(PlayerClass.values())
                .filter(c -> c.id.equals(normalized))
                .findFirst()
                .orElse(null);
    }

    private static void unlockClass(ServerPlayer player, String id) {
        Set<String> unlocked = getUnlockedClasses(player);
        unlocked.add(id);
        player.getPersistentData().putString(TAG_UNLOCKED_CLASSES, String.join(",", unlocked));
    }

    private static Set<String> getUnlockedClasses(ServerPlayer player) {
        ensureDefaults(player);
        String raw = player.getPersistentData().getString(TAG_UNLOCKED_CLASSES);
        if (raw == null || raw.isBlank()) return new HashSet<>(Set.of(PlayerClass.WARRIOR.id));
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static void applyPlayerModifiers(ServerPlayer player) {
        int level = getLevel(player);
        PlayerClass klass = getClass(player);

        double levelHealthBonus = (level - 1) * 0.40;
        double levelAttackBonus = (level - 1) * 0.15;

        applyAdd(player.getAttribute(Attributes.MAX_HEALTH), LEVEL_HEALTH_MOD, "ec_level_health", levelHealthBonus);
        applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), LEVEL_ATTACK_MOD, "ec_level_attack", levelAttackBonus);

        double classHealthFlat = Math.max(0.0, player.getAttributeValue(Attributes.MAX_HEALTH) * klass.healthPctBonus * 0.20);
        applyAdd(player.getAttribute(Attributes.MAX_HEALTH), CLASS_HEALTH_MOD, "ec_class_health", classHealthFlat);
        applyAdd(player.getAttribute(Attributes.ATTACK_DAMAGE), CLASS_ATTACK_MOD, "ec_class_attack", klass.flatAttackBonus);
        applyAdd(player.getAttribute(Attributes.MOVEMENT_SPEED), CLASS_SPEED_MOD, "ec_class_speed", klass.speedBonus);
        applyAdd(player.getAttribute(Attributes.LUCK), CLASS_LUCK_MOD, "ec_class_luck", klass.luckBonus);

        double max = player.getMaxHealth();
        if (player.getHealth() > max) player.setHealth((float) max);
    }

    private static void applyAdd(AttributeInstance attr, UUID id, String name, double amount) {
        if (attr == null) return;
        AttributeModifier existing = attr.getModifier(id);
        if (existing != null) attr.removeModifier(existing);
        if (Math.abs(amount) < 0.00001D) return;
        attr.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }
}
