package com.extremecraft.modules.loader;

import com.extremecraft.modules.data.ModuleAbilityDefinition;
import com.extremecraft.modules.data.ModuleTrigger;
import com.extremecraft.modules.registry.ModuleAbilityRegistry;
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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loader for module-triggered ability definitions.
 *
 * <p>Canonical module trigger content now lives in {@code data/extremecraft/module_abilities}.
 * A warned compatibility fallback still reads trigger-bearing files from
 * {@code data/extremecraft/abilities} so older packs do not break immediately.</p>
 */
public class ModuleAbilityLoader {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader("module_abilities", false));
        event.addListener(new Loader("abilities", true));
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private final String rootPath;
        private final boolean legacyFallback;

        private Loader(String rootPath, boolean legacyFallback) {
            super(GSON, rootPath);
            this.rootPath = rootPath;
            this.legacyFallback = legacyFallback;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, ModuleAbilityDefinition> loaded = new LinkedHashMap<>();
            int malformed = 0;
            int skippedNonModuleEntries = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    if (!entry.getValue().isJsonObject()) {
                        malformed++;
                        LOGGER.warn("[Module] Skipping non-object module ability {}", entry.getKey());
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    if (legacyFallback && !looksLikeLegacyModuleAbility(root)) {
                        skippedNonModuleEntries++;
                        continue;
                    }

                    String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase(Locale.ROOT);
                    if (id.contains("/")) {
                        id = id.substring(id.lastIndexOf('/') + 1);
                    }
                    if (id.isBlank()) {
                        LOGGER.warn("[Module] Skipping module ability with blank id from {}", entry.getKey());
                        continue;
                    }

                    ModuleTrigger trigger = ModuleTrigger.byName(GsonHelper.getAsString(root, "trigger", "passive"));
                    int cooldownTicks = readInt(root, "cooldown_ticks", "cd_ticks", 0);
                    int manaCost = readInt(root, "mana_cost", "mana", 0);

                    Map<String, Double> scaling = new LinkedHashMap<>();
                    if (root.has("scaling") && root.get("scaling").isJsonObject()) {
                        JsonObject scalingJson = root.getAsJsonObject("scaling");
                        for (Map.Entry<String, JsonElement> scalingEntry : scalingJson.entrySet()) {
                            if (scalingEntry.getValue().isJsonPrimitive() && scalingEntry.getValue().getAsJsonPrimitive().isNumber()) {
                                scaling.put(scalingEntry.getKey().trim().toLowerCase(Locale.ROOT), scalingEntry.getValue().getAsDouble());
                            }
                        }
                    }

                    ModuleAbilityDefinition previous = loaded.put(id,
                            new ModuleAbilityDefinition(id, trigger, Math.max(0, cooldownTicks), Math.max(0, manaCost), Map.copyOf(scaling)));
                    if (previous != null) {
                        LOGGER.warn("[Module] Duplicate module ability id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }
                } catch (RuntimeException ex) {
                    malformed++;
                    LOGGER.warn("[Module] Skipping malformed module ability {} ({}): {}",
                            entry.getKey(), ex.getClass().getSimpleName(), ex.getMessage());
                }
            }

            if (!legacyFallback) {
                ModuleAbilityRegistry.replaceAll(loaded);
                LOGGER.info("[Module] Reloaded canonical module abilities from {}: loaded={}, malformed={}", rootPath, loaded.size(), malformed);
                return;
            }

            int merged = ModuleAbilityRegistry.mergeMissing(loaded);
            int shadowed = Math.max(0, loaded.size() - merged);
            if (merged > 0) {
                LOGGER.warn("[Module] Loaded {} legacy module ability definition(s) from data/extremecraft/abilities. Canonical path is data/extremecraft/module_abilities.", merged);
            }
            if (shadowed > 0) {
                LOGGER.info("[Module] Ignored {} legacy module ability definition(s) shadowed by canonical data/extremecraft/module_abilities entries.", shadowed);
            }
            LOGGER.info("[Module] Legacy module ability fallback scan from {}: candidates={}, skipped_non_module={}, malformed={}",
                    rootPath, loaded.size(), skippedNonModuleEntries, malformed);
        }

        private static boolean looksLikeLegacyModuleAbility(JsonObject root) {
            return root.has("trigger");
        }

        private static int readInt(JsonObject root, String primary, String legacy, int fallback) {
            if (root.has(primary) && root.get(primary).isJsonPrimitive() && root.get(primary).getAsJsonPrimitive().isNumber()) {
                return root.get(primary).getAsInt();
            }
            if (root.has(legacy) && root.get(legacy).isJsonPrimitive() && root.get(legacy).getAsJsonPrimitive().isNumber()) {
                return root.get(legacy).getAsInt();
            }
            return fallback;
        }
    }
}
