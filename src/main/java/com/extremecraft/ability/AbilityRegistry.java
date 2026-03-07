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

/**
 * Central ability registry combining data-driven definitions and executable runtime handlers.
 *
 * <p>Datapack JSON definitions are reloaded through Forge resource reload events, while
 * runtime ability executors are registered programmatically at startup or by integrations.
 * The two layers intentionally coexist so balancing data can evolve independently from
 * Java effect execution code.</p>
 */
public final class AbilityRegistry {
    private static final Map<String, AbilityDefinition> DEFINITIONS = new LinkedHashMap<>();
    private static List<AbilityDefinition> CACHED_LIST = List.of();

    private static final Map<String, Ability> RUNTIME_ABILITIES = new LinkedHashMap<>();
    private static List<Ability> CACHED_RUNTIME = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized void register(String id, Ability ability) {
        if (ability == null) {
            return;
        }

        String key = normalize(id == null || id.isBlank() ? ability.getId() : id);
        if (key.isBlank()) {
            return;
        }

        RUNTIME_ABILITIES.put(key, ability);
        CACHED_RUNTIME = List.copyOf(RUNTIME_ABILITIES.values());
    }

    /**
     * Extension hook for other modules/mods to add data-driven ability definitions at runtime.
     */
    public static synchronized void registerDefinition(AbilityDefinition definition) {
        if (definition == null || normalize(definition.id()).isBlank()) {
            return;
        }

        DEFINITIONS.put(normalize(definition.id()), definition);
        CACHED_LIST = List.copyOf(DEFINITIONS.values());
    }

    public static synchronized Ability runtime(String id) {
        String key = normalize(id);
        return key.isBlank() ? null : RUNTIME_ABILITIES.get(key);
    }

    public static synchronized Collection<Ability> runtimeAbilities() {
        return CACHED_RUNTIME;
    }

    public static synchronized AbilityDefinition get(String id) {
        String key = normalize(id);
        return key.isBlank() ? null : DEFINITIONS.get(key);
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

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase();
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

