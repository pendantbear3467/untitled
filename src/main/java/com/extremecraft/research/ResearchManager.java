package com.extremecraft.research;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResearchManager extends SimpleJsonResourceReloadListener {
    /**
     * Runtime research definition loader for {@code data/extremecraft/research/*.json}.
     * <p>
     * This listener is the gameplay-facing source for staged unlock rules consumed by progression
     * systems, and now enforces an array-based unlock schema to keep datapack contracts consistent.
     */
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ResearchDefinition> RESEARCH = new LinkedHashMap<>();

    public ResearchManager() {
        super(GSON, "research");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ResearchDefinition> loaded = new LinkedHashMap<>();
        int malformed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "research_definition");
                String id = normalizeId(GsonHelper.getAsString(json, "research", entry.getKey().getPath()));
                if (id.isBlank()) {
                    LOGGER.warn("[Research] Skipping research with blank id from {}", entry.getKey());
                    continue;
                }

                String stageName = GsonHelper.getAsString(json, "required_stage", GsonHelper.getAsString(json, "stage", "PRIMITIVE"));
                ProgressionStage requiredStage = ProgressionStage.byName(stageName).orElse(ProgressionStage.PRIMITIVE);

                List<String> unlocks = new ArrayList<>();
                JsonElement unlocksElement = json.get("unlocks");
                if (unlocksElement != null && !unlocksElement.isJsonNull()) {
                    if (unlocksElement.isJsonArray()) {
                        for (JsonElement unlockValue : unlocksElement.getAsJsonArray()) {
                            if (unlockValue == null || !unlockValue.isJsonPrimitive()) {
                                LOGGER.warn("[Research] Ignoring non-string unlock entry in {}", entry.getKey());
                                continue;
                            }

                            String unlock = unlockValue.getAsString().trim();
                            if (!unlock.isBlank()) {
                                unlocks.add(unlock);
                            }
                        }
                    } else if (unlocksElement.isJsonPrimitive()) {
                        String singleUnlock = unlocksElement.getAsString().trim();
                        if (!singleUnlock.isBlank()) {
                            unlocks.add(singleUnlock);
                        }
                    } else {
                        LOGGER.warn("[Research] Ignoring malformed unlocks section in {} (expected array or string)", entry.getKey());
                    }
                }

                ResearchDefinition previous = loaded.put(id, new ResearchDefinition(id, requiredStage, List.copyOf(unlocks)));
                if (previous != null) {
                    LOGGER.warn("[Research] Duplicate research id '{}' detected; keeping latest from {}", id, entry.getKey());
                }
            } catch (RuntimeException ex) {
                malformed++;
                LOGGER.warn("[Research] Skipping malformed research {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (!map.isEmpty() && loaded.isEmpty()) {
            synchronized (RESEARCH) {
                if (!RESEARCH.isEmpty()) {
                    LOGGER.warn("[Research] Reload produced no valid entries; keeping previous registry (malformed={})", malformed);
                    return;
                }
            }
        }

        synchronized (RESEARCH) {
            RESEARCH.clear();
            RESEARCH.putAll(loaded);
        }
        LOGGER.info("[Research] Reloaded research definitions: loaded={}, malformed={}", loaded.size(), malformed);
    }

    public static ResearchDefinition byId(String id) {
        if (id == null) {
            return null;
        }

        synchronized (RESEARCH) {
            return RESEARCH.get(id.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static Collection<ResearchDefinition> all() {
        synchronized (RESEARCH) {
            return List.copyOf(RESEARCH.values());
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
}
