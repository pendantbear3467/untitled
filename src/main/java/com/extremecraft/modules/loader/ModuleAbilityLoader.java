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

public class ModuleAbilityLoader {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private Loader() {
            super(GSON, "abilities");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, ModuleAbilityDefinition> loaded = new LinkedHashMap<>();
            int malformed = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    if (!entry.getValue().isJsonObject()) {
                        malformed++;
                        LOGGER.warn("[Module] Skipping non-object module ability {}", entry.getKey());
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase(Locale.ROOT);
                    if (id.contains("/")) {
                        id = id.substring(id.lastIndexOf('/') + 1);
                    }
                    if (id.isBlank()) {
                        LOGGER.warn("[Module] Skipping module ability with blank id from {}", entry.getKey());
                        continue;
                    }

                    ModuleTrigger trigger = ModuleTrigger.byName(GsonHelper.getAsString(root, "trigger", "passive"));
                    int cooldownTicks = Math.max(0, GsonHelper.getAsInt(root, "cooldown_ticks", 0));
                    int manaCost = Math.max(0, GsonHelper.getAsInt(root, "mana_cost", 0));

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
                            new ModuleAbilityDefinition(id, trigger, cooldownTicks, manaCost, Map.copyOf(scaling)));
                    if (previous != null) {
                        LOGGER.warn("[Module] Duplicate module ability id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }
                } catch (RuntimeException ex) {
                    malformed++;
                    LOGGER.warn("[Module] Skipping malformed module ability {} ({}): {}",
                            entry.getKey(), ex.getClass().getSimpleName(), ex.getMessage());
                }
            }

            ModuleAbilityRegistry.replaceAll(loaded);
            LOGGER.info("[Module] Reloaded module abilities: loaded={}, malformed={}", loaded.size(), malformed);
        }
    }
}
