package com.extremecraft.progression;

import com.google.gson.Gson;
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

public final class ProgressionRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ProgressionDefinition> DEFINITIONS = new LinkedHashMap<>();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized Collection<ProgressionDefinition> all() {
        return List.copyOf(DEFINITIONS.values());
    }

    public static synchronized ProgressionDefinition get(String id) {
        return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase(Locale.ROOT));
    }

    private static synchronized void replace(Map<String, ProgressionDefinition> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        private Loader() {
            super(GSON, "progression");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, ProgressionDefinition> loaded = new LinkedHashMap<>();
            int malformed = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    if (!entry.getValue().isJsonObject()) {
                        malformed++;
                        LOGGER.warn("[Progression] Skipping non-object definition {}", entry.getKey());
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    String id = normalizeId(GsonHelper.getAsString(root, "id", entry.getKey().getPath()));
                    if (id.isBlank()) {
                        LOGGER.warn("[Progression] Skipping definition with blank id from {}", entry.getKey());
                        continue;
                    }

                    int baseXp = Math.max(1, GsonHelper.getAsInt(root, "base_xp", 100));
                    double levelMultiplier = Math.max(1.0D, GsonHelper.getAsDouble(root, "level_multiplier", 1.15D));

                    Map<String, Double> growth = new LinkedHashMap<>();
                    if (root.has("stat_growth") && root.get("stat_growth").isJsonObject()) {
                        JsonObject statGrowth = root.getAsJsonObject("stat_growth");
                        for (Map.Entry<String, JsonElement> growthEntry : statGrowth.entrySet()) {
                            if (growthEntry.getValue().isJsonPrimitive() && growthEntry.getValue().getAsJsonPrimitive().isNumber()) {
                                growth.put(growthEntry.getKey().trim().toLowerCase(Locale.ROOT), growthEntry.getValue().getAsDouble());
                            }
                        }
                    }

                    ProgressionDefinition previous = loaded.put(id, new ProgressionDefinition(id, baseXp, levelMultiplier, Map.copyOf(growth)));
                    if (previous != null) {
                        LOGGER.warn("[Progression] Duplicate definition id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }
                } catch (RuntimeException ex) {
                    malformed++;
                    LOGGER.warn("[Progression] Skipping malformed definition {}: {}", entry.getKey(), ex.getMessage());
                }
            }

            if (!jsonMap.isEmpty() && loaded.isEmpty() && !DEFINITIONS.isEmpty()) {
                LOGGER.warn("[Progression] Reload produced no valid definitions; keeping previous registry (malformed={})", malformed);
                return;
            }

            replace(loaded);
            LOGGER.info("[Progression] Reloaded progression definitions: loaded={}, malformed={}", loaded.size(), malformed);
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
}
