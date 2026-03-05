package com.extremecraft.progression;

import com.extremecraft.progression.stage.ProgressionStage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StageDataLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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
        UNLOCK_TO_STAGE.clear();
        STAGE_TO_UNLOCKS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "progression_stage");
            ProgressionStage stage = ProgressionStage.byName(GsonHelper.getAsString(json, "stage", "PRIMITIVE"))
                    .orElse(ProgressionStage.PRIMITIVE);

            JsonArray unlocks = GsonHelper.getAsJsonArray(json, "unlocks", new JsonArray());
            Set<String> values = STAGE_TO_UNLOCKS.computeIfAbsent(stage, key -> new LinkedHashSet<>());
            for (JsonElement unlockEntry : unlocks) {
                String unlock = unlockEntry.getAsString();
                if (unlock.isBlank()) {
                    continue;
                }

                values.add(unlock);
                UNLOCK_TO_STAGE.put(unlock, stage);
            }
        }
    }

    public static Optional<ProgressionStage> requiredStageForUnlock(String unlockId) {
        return Optional.ofNullable(UNLOCK_TO_STAGE.get(unlockId));
    }

    public static Collection<String> unlocksForStage(ProgressionStage stage) {
        return STAGE_TO_UNLOCKS.getOrDefault(stage, Set.of());
    }
}
