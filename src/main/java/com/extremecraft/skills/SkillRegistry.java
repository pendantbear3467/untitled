package com.extremecraft.skills;

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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SkillRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, SkillDefinition> SKILLS = new LinkedHashMap<>();

    public SkillRegistry() {
        super(GSON, "skills");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, SkillDefinition> loaded = new LinkedHashMap<>();
        int malformed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "skill_definition");
                String id = normalizeSkillId(GsonHelper.getAsString(json, "skill", entry.getKey().getPath()));
                if (id.isBlank()) {
                    LOGGER.warn("[Skills] Skipping skill definition with blank id from {}", entry.getKey());
                    continue;
                }

                int maxLevel = Math.max(1, GsonHelper.getAsInt(json, "max_level", 100));
                double bonusPerLevel = Math.max(0.0D, GsonHelper.getAsDouble(json, "bonus_per_level", 0.0D));
                SkillDefinition previous = loaded.put(id, new SkillDefinition(id, maxLevel, bonusPerLevel));
                if (previous != null) {
                    LOGGER.warn("[Skills] Duplicate skill id '{}' detected; keeping latest from {}", id, entry.getKey());
                }
            } catch (RuntimeException ex) {
                malformed++;
                LOGGER.warn("[Skills] Skipping malformed skill definition {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (!map.isEmpty() && loaded.isEmpty()) {
            synchronized (SKILLS) {
                if (!SKILLS.isEmpty()) {
                    LOGGER.warn("[Skills] Reload produced no valid entries; keeping previous skill registry (malformed={})", malformed);
                    return;
                }
            }
        }

        loaded.putIfAbsent("mining", new SkillDefinition("mining", 100, 0.03D));
        loaded.putIfAbsent("combat", new SkillDefinition("combat", 100, 0.02D));
        loaded.putIfAbsent("engineering", new SkillDefinition("engineering", 100, 0.02D));
        loaded.putIfAbsent("arcane", new SkillDefinition("arcane", 100, 0.02D));

        synchronized (SKILLS) {
            SKILLS.clear();
            SKILLS.putAll(loaded);
        }
        LOGGER.info("[Skills] Reloaded skills: loaded={}, malformed={}", loaded.size(), malformed);
    }

    public static SkillDefinition byId(String id) {
        if (id == null) {
            return null;
        }

        synchronized (SKILLS) {
            return SKILLS.get(id.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static Collection<SkillDefinition> all() {
        synchronized (SKILLS) {
            return List.copyOf(SKILLS.values());
        }
    }

    private static String normalizeSkillId(String raw) {
        if (raw == null) {
            return "";
        }

        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (id.contains("/")) {
            id = id.substring(id.lastIndexOf('/') + 1);
        }
        return id;
    }
}
