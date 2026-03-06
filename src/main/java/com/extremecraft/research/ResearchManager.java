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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;

public class ResearchManager extends SimpleJsonResourceReloadListener {
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
        RESEARCH.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "research_definition");
            String id = GsonHelper.getAsString(json, "research", entry.getKey().getPath());
            ProgressionStage requiredStage = ProgressionStage.byName(GsonHelper.getAsString(json, "required_stage", "PRIMITIVE"))
                    .orElse(ProgressionStage.PRIMITIVE);

            List<String> unlocks = new ArrayList<>();
            JsonElement unlocksElement = json.get("unlocks");
            if (unlocksElement != null && !unlocksElement.isJsonNull()) {
                if (unlocksElement.isJsonArray()) {
                    for (JsonElement unlockValue : unlocksElement.getAsJsonArray()) {
                        String unlock = unlockValue.getAsString();
                        if (!unlock.isBlank()) {
                            unlocks.add(unlock);
                        }
                    }
                } else if (unlocksElement.isJsonPrimitive()) {
                    String unlock = unlocksElement.getAsString();
                    if (!unlock.isBlank()) {
                        unlocks.add(unlock);
                    }
                } else {
                    LOGGER.warn("Skipping invalid unlocks format in research {} from {}", id, entry.getKey());
                }
            }

            RESEARCH.put(id, new ResearchDefinition(id, requiredStage, List.copyOf(unlocks)));
        }
    }

    public static ResearchDefinition byId(String id) {
        return RESEARCH.get(id);
    }

    public static Collection<ResearchDefinition> all() {
        return RESEARCH.values();
    }
}
