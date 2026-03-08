package com.extremecraft.progression;

import com.extremecraft.progression.stage.ProgressionStage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StageDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, ProgressionStage> UNLOCK_TO_STAGE = new HashMap<>();
    private static final Map<ProgressionStage, Set<String>> STAGE_TO_UNLOCKS = new EnumMap<>(ProgressionStage.class);

    public StageDataLoader() {
        super(GSON, "progression/stages");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, ProgressionStage> loadedUnlockToStage = new HashMap<>();
        Map<ProgressionStage, Set<String>> loadedStageToUnlocks = new EnumMap<>(ProgressionStage.class);
        int malformed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "progression_stage");
                String stageName = GsonHelper.getAsString(json, "stage", "PRIMITIVE");
                ProgressionStage stage = ProgressionStage.byName(stageName).orElse(ProgressionStage.PRIMITIVE);

                Set<String> values = loadedStageToUnlocks.computeIfAbsent(stage, key -> new LinkedHashSet<>());
                JsonElement unlocksElement = json.get("unlocks");
                if (unlocksElement == null || unlocksElement.isJsonNull()) {
                    continue;
                }

                JsonArray unlocksArray;
                if (unlocksElement.isJsonArray()) {
                    unlocksArray = unlocksElement.getAsJsonArray();
                } else if (unlocksElement.isJsonPrimitive()) {
                    unlocksArray = new JsonArray();
                    unlocksArray.add(unlocksElement.getAsString());
                } else {
                    LOGGER.warn("[Stage] Ignoring malformed unlock list in {} (expected array or string)", entry.getKey());
                    continue;
                }

                for (JsonElement unlockEntry : unlocksArray) {
                    if (unlockEntry == null || !unlockEntry.isJsonPrimitive()) {
                        LOGGER.warn("[Stage] Ignoring non-string unlock entry in {}", entry.getKey());
                        continue;
                    }

                    String unlock = normalizeUnlock(unlockEntry.getAsString());
                    if (unlock.isBlank()) {
                        continue;
                    }

                    values.add(unlock);
                    loadedUnlockToStage.put(unlock, stage);
                }
            } catch (RuntimeException ex) {
                malformed++;
                LOGGER.warn("[Stage] Skipping malformed stage definition {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (!map.isEmpty() && loadedUnlockToStage.isEmpty()) {
            synchronized (UNLOCK_TO_STAGE) {
                if (!UNLOCK_TO_STAGE.isEmpty()) {
                    LOGGER.warn("[Stage] Reload produced no valid entries; keeping previous stage mapping (malformed={})", malformed);
                    return;
                }
            }
        }

        synchronized (UNLOCK_TO_STAGE) {
            UNLOCK_TO_STAGE.clear();
            UNLOCK_TO_STAGE.putAll(loadedUnlockToStage);
            STAGE_TO_UNLOCKS.clear();
            loadedStageToUnlocks.forEach((stage, unlocks) -> STAGE_TO_UNLOCKS.put(stage, Set.copyOf(unlocks)));
        }

        LOGGER.info("[Stage] Reloaded stage unlock mapping: unlocks={}, stages={}, malformed={}",
                loadedUnlockToStage.size(), loadedStageToUnlocks.size(), malformed);
    }

    public static Optional<ProgressionStage> requiredStageForUnlock(String unlockId) {
        if (unlockId == null) {
            return Optional.empty();
        }

        synchronized (UNLOCK_TO_STAGE) {
            return Optional.ofNullable(UNLOCK_TO_STAGE.get(normalizeUnlock(unlockId)));
        }
    }

    public static Collection<String> unlocksForStage(ProgressionStage stage) {
        synchronized (UNLOCK_TO_STAGE) {
            return STAGE_TO_UNLOCKS.getOrDefault(stage, Set.of());
        }
    }

    private static String normalizeUnlock(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
