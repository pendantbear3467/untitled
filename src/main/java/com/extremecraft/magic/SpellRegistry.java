package com.extremecraft.magic;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SpellRegistry {
    private static final Map<String, SpellDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static List<SpellDefinition> CACHE = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized SpellDefinition get(String id) {
        return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase());
    }

    public static synchronized Collection<SpellDefinition> all() {
        return CACHE;
    }

    public static synchronized int size() {
        return DEFINITIONS.size();
    }

    private static synchronized void replaceAll(Map<String, SpellDefinition> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
        CACHE = List.copyOf(DEFINITIONS.values());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        private Loader() {
            super(GSON, "spells");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, SpellDefinition> loaded = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                SpellDefinition definition = SpellDefinition.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                if (definition.id().isBlank()) {
                    continue;
                }

                loaded.put(definition.id(), definition);
            }

            ExtremeCraftAPI.spells().forEach(apiSpell -> loaded.putIfAbsent(apiSpell.id(),
                    new SpellDefinition(apiSpell.id(), SpellDefinition.SpellType.byName(apiSpell.type()), 0, 0, 1.5D, 0.0D,
                            0, "", List.of(), List.of())));

            replaceAll(loaded);
        }
    }
}
