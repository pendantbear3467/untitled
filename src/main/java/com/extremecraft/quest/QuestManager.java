package com.extremecraft.quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class QuestManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, QuestDefinition> QUESTS = new LinkedHashMap<>();

    public QuestManager() {
        super(GSON, "extremecraft_quests");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        QUESTS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "quest");
            String id = GsonHelper.getAsString(json, "id", entry.getKey().getPath());
            String title = GsonHelper.getAsString(json, "title", id);
            QuestType type = QuestType.valueOf(GsonHelper.getAsString(json, "type", "KILL").toUpperCase());
            int target = Math.max(1, GsonHelper.getAsInt(json, "target", 1));

            JsonObject rewards = GsonHelper.getAsJsonObject(json, "rewards", new JsonObject());
            int xp = Math.max(0, GsonHelper.getAsInt(rewards, "xp", 0));
            int playerSkill = Math.max(0, GsonHelper.getAsInt(rewards, "player_skill_points", 0));
            int classSkill = Math.max(0, GsonHelper.getAsInt(rewards, "class_skill_points", 0));
            String unlockClass = GsonHelper.getAsString(rewards, "unlock_class", "");

            QUESTS.put(id, new QuestDefinition(id, title, type, target, xp, playerSkill, classSkill, unlockClass));
        }

        if (QUESTS.isEmpty()) {
            seedDefaultQuests();
        }
    }

    private static void seedDefaultQuests() {
        QUESTS.put("fighter_trial", new QuestDefinition("fighter_trial", "Fighter Trial", QuestType.KILL, 25, 200, 1, 1, "fighter"));
        QUESTS.put("miner_trial", new QuestDefinition("miner_trial", "Miner Trial", QuestType.COLLECTION, 64, 150, 1, 1, "miner"));
        QUESTS.put("explorer_trial", new QuestDefinition("explorer_trial", "Explorer Trial", QuestType.EXPLORATION, 10, 180, 2, 1, "explorer"));
        QUESTS.put("scientist_trial", new QuestDefinition("scientist_trial", "Scientist Trial", QuestType.CRAFTING, 20, 220, 2, 2, "scientist"));
        QUESTS.put("medic_trial", new QuestDefinition("medic_trial", "Medic Trial", QuestType.BOSS, 1, 300, 2, 2, "medic"));
        QUESTS.put("trader_trial", new QuestDefinition("trader_trial", "Trader Trial", QuestType.COLLECTION, 128, 250, 2, 2, "trader"));
    }

    public static Collection<QuestDefinition> all() {
        return QUESTS.values();
    }

    public static QuestDefinition byId(String id) {
        return QUESTS.get(id);
    }
}
