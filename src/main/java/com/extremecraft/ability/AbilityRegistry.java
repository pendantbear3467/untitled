package com.extremecraft.ability;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AbilityRegistry {
    private static final Map<String, AbilityDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static List<AbilityDefinition> CACHED_LIST = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized AbilityDefinition get(String id) {
        return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase());
    }

    public static synchronized Collection<AbilityDefinition> all() {
        return CACHED_LIST;
    }

    public static synchronized int size() {
        return DEFINITIONS.size();
    }

    private static synchronized void replaceAll(Map<String, AbilityDefinition> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
        CACHED_LIST = List.copyOf(DEFINITIONS.values());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        private Loader() {
            super(GSON, "abilities");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, AbilityDefinition> loaded = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject root = entry.getValue().getAsJsonObject();
                AbilityDefinition definition = AbilityDefinition.fromJson(entry.getKey(), root);
                if (definition.id().isBlank()) {
                    continue;
                }

                loaded.put(definition.id(), definition);
            }

            // Modules can still register API-level abilities; bridge them into runtime defaults.
            ExtremeCraftAPI.abilities().forEach(apiDefinition -> loaded.putIfAbsent(apiDefinition.id(),
                    new AbilityDefinition(
                            apiDefinition.id(),
                            0,
                            0,
                            AbilityDefinition.TargetType.SELF,
                            0.0D,
                            8.0D,
                            "",
                            new ArrayList<>()
                    )));

            replaceAll(loaded);
        }
    }
}
