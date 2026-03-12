package com.extremecraft.quest;

import com.extremecraft.progression.classsystem.ClassIdResolver;
import com.extremecraft.progression.stage.ProgressionStage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    public QuestManager() {
        super(GSON, "extremecraft_quests");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(@Nonnull Map<ResourceLocation, JsonElement> map, @Nonnull ResourceManager manager, @Nonnull ProfilerFiller profiler) {
        Map<String, QuestDefinition> loaded = new LinkedHashMap<>();
        int malformed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "quest");
                String id = normalizeId(GsonHelper.getAsString(json, "id", entry.getKey().getPath()));
                if (id.isBlank()) {
                    LOGGER.warn("[Quest] Skipping quest with blank id from {}", entry.getKey());
                    continue;
                }

                String title = trimOrDefault(GsonHelper.getAsString(json, "title", id), id);
                QuestType type = parseQuestType(trimOrDefault(GsonHelper.getAsString(json, "type", "KILL"), "KILL"), id, entry.getKey());
                int target = Math.max(1, GsonHelper.getAsInt(json, "target", 1));

                JsonObject rewards = GsonHelper.getAsJsonObject(json, "rewards", new JsonObject());
                int xp = Math.max(0, GsonHelper.getAsInt(rewards, "xp", 0));
                int playerSkill = Math.max(0, GsonHelper.getAsInt(rewards, "player_skill_points", 0));
                int classSkill = Math.max(0, GsonHelper.getAsInt(rewards, "class_skill_points", 0));
                String unlockClass = normalizeClassId(GsonHelper.getAsString(rewards, "unlock_class", ""));
                String unlockStage = trimOrDefault(GsonHelper.getAsString(rewards, "unlock_stage", ""), "").toUpperCase(Locale.ROOT);
                if (!unlockStage.isBlank() && ProgressionStage.byName(unlockStage).isEmpty()) {
                    LOGGER.warn("[Quest] Unknown unlock stage '{}' for {} from {}; clearing reward stage", unlockStage, id, entry.getKey());
                    unlockStage = "";
                }

                QuestDefinition previous = loaded.put(id,
                        new QuestDefinition(id, title, type, target, xp, playerSkill, classSkill, unlockClass, unlockStage));
                if (previous != null) {
                    LOGGER.warn("[Quest] Duplicate quest id '{}' detected; keeping latest from {}", id, entry.getKey());
                }
            } catch (RuntimeException ex) {
                malformed++;
                LOGGER.warn("[Quest] Skipping malformed quest {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (loaded.isEmpty()) {
            if (!map.isEmpty()) {
                synchronized (QUESTS) {
                    if (!QUESTS.isEmpty()) {
                        LOGGER.warn("[Quest] Reload produced no valid entries; keeping previous quest registry (malformed={})", malformed);
                        return;
                    }
                }
            }

            seedDefaultQuests(loaded);
        }

        synchronized (QUESTS) {
            QUESTS.clear();
            QUESTS.putAll(loaded);
        }
        LOGGER.info("[Quest] Reloaded quests: loaded={}, malformed={}", loaded.size(), malformed);
    }

    private static void seedDefaultQuests(Map<String, QuestDefinition> target) {
        target.put("fighter_trial", new QuestDefinition("fighter_trial", "Fighter Trial", QuestType.KILL, 25, 200, 1, 1, "warrior", ""));
        target.put("miner_trial", new QuestDefinition("miner_trial", "Miner Trial", QuestType.COLLECTION, 64, 150, 1, 1, "engineer", ""));
        target.put("explorer_trial", new QuestDefinition("explorer_trial", "Explorer Trial", QuestType.EXPLORATION, 10, 180, 2, 1, "ranger", ""));
        target.put("scientist_trial", new QuestDefinition("scientist_trial", "Scientist Trial", QuestType.CRAFTING, 20, 220, 2, 2, "technomancer", "INDUSTRIAL"));
        target.put("medic_trial", new QuestDefinition("medic_trial", "Medic Trial", QuestType.BOSS, 1, 300, 2, 2, "warden", ""));
        target.put("trader_trial", new QuestDefinition("trader_trial", "Trader Trial", QuestType.COLLECTION, 128, 250, 2, 2, "chronomancer", ""));
    }

    public static Collection<QuestDefinition> all() {
        synchronized (QUESTS) {
            return List.copyOf(QUESTS.values());
        }
    }

    public static QuestDefinition byId(String id) {
        if (id == null) {
            return null;
        }

        synchronized (QUESTS) {
            return QUESTS.get(id.trim().toLowerCase(Locale.ROOT));
        }
    }

    private static QuestType parseQuestType(String rawType, String questId, ResourceLocation source) {
        if (rawType == null || rawType.isBlank()) {
            return QuestType.KILL;
        }

        try {
            return QuestType.valueOf(rawType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            LOGGER.warn("[Quest] Unknown quest type '{}' for {} from {}; defaulting to KILL", rawType, questId, source);
            return QuestType.KILL;
        }
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }

        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (id.contains("/")) {
            id = id.substring(id.lastIndexOf('/') + 1);
        }
        return id;
    }

    private static String normalizeClassId(String raw) {
        return ClassIdResolver.normalizeCanonical(normalizeId(raw));
    }

    private static String trimOrDefault(String value, String fallback) {
        if (value == null) {
            return fallback;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
