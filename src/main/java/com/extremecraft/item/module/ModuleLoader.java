package com.extremecraft.item.module;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Loads module definitions from data/extremecraft/modules/*.json.
 */
public class ModuleLoader {
    private static final Logger LOGGER = Logger.getLogger("ExtremeCraft");
    private static final Gson GSON = new Gson();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(GSON, "modules");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
            Map<String, ModuleDefinition> loaded = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject json = entry.getValue().getAsJsonObject();
                String id = GsonHelper.getAsString(json, "id", entry.getKey().getPath()).trim().toLowerCase();
                if (id.contains("/")) {
                    id = id.substring(id.lastIndexOf('/') + 1);
                }
                if (id.isBlank()) {
                    continue;
                }

                ModuleType type = ModuleType.byName(GsonHelper.getAsString(json, "type", "universal"));
                int maxLevel = Math.max(1, GsonHelper.getAsInt(json, "max_level", 1));

                Map<String, Float> effects = new LinkedHashMap<>();
                if (json.has("effects") && json.get("effects").isJsonObject()) {
                    JsonObject effectJson = json.getAsJsonObject("effects");
                    for (Map.Entry<String, JsonElement> effect : effectJson.entrySet()) {
                        if (effect.getValue().isJsonPrimitive() && effect.getValue().getAsJsonPrimitive().isNumber()) {
                            effects.put(effect.getKey().trim().toLowerCase(), effect.getValue().getAsFloat());
                        }
                    }
                }

                loaded.put(id, new ModuleDefinition(id, type, maxLevel, Map.copyOf(effects)));
            }

            ModuleRegistry.replaceAll(loaded);
            LOGGER.info("[Modules] Loaded " + loaded.size() + " module definition(s).");
        }
    }
}
