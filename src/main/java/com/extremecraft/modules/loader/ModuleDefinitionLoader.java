package com.extremecraft.modules.loader;

import com.extremecraft.modules.data.ModuleDefinition;
import com.extremecraft.modules.data.ModuleType;
import com.extremecraft.modules.registry.ArmorModuleRegistry;
import com.extremecraft.modules.registry.ToolModuleRegistry;
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
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ModuleDefinitionLoader {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader("armor_modules", ModuleType.ARMOR));
        event.addListener(new Loader("tool_modules", ModuleType.TOOL));
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private final ModuleType type;

        private Loader(String path, ModuleType type) {
            super(GSON, path);
            this.type = type;
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, ModuleDefinition> loaded = new LinkedHashMap<>();
            int malformed = 0;

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    if (!entry.getValue().isJsonObject()) {
                        malformed++;
                        LOGGER.warn("[Module] Skipping non-object module definition {}", entry.getKey());
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase(Locale.ROOT);
                    if (id.contains("/")) {
                        id = id.substring(id.lastIndexOf('/') + 1);
                    }
                    if (id.isBlank()) {
                        LOGGER.warn("[Module] Skipping module with blank id from {}", entry.getKey());
                        continue;
                    }

                    int slotCost = readInt(root, "slot_cost", "slots", 1);
                    String requiredSkillNode = readString(root, "required_skill_node", "required_skill").trim().toLowerCase(Locale.ROOT);

                    Map<String, Float> statModifiers = new LinkedHashMap<>();
                    if (root.has("stat_modifiers") && root.get("stat_modifiers").isJsonObject()) {
                        JsonObject statJson = root.getAsJsonObject("stat_modifiers");
                        for (Map.Entry<String, JsonElement> statEntry : statJson.entrySet()) {
                            if (statEntry.getValue().isJsonPrimitive() && statEntry.getValue().getAsJsonPrimitive().isNumber()) {
                                statModifiers.put(statEntry.getKey().trim().toLowerCase(Locale.ROOT), statEntry.getValue().getAsFloat());
                            }
                        }
                    }

                    Set<String> abilities = new LinkedHashSet<>();
                    if (root.has("abilities") && root.get("abilities").isJsonArray()) {
                        for (JsonElement ability : root.getAsJsonArray("abilities")) {
                            if (!ability.isJsonPrimitive() || !ability.getAsJsonPrimitive().isString()) {
                                LOGGER.warn("[Module] Ignoring non-string module ability entry in {}", entry.getKey());
                                continue;
                            }

                            String abilityId = ability.getAsString().trim().toLowerCase(Locale.ROOT);
                            if (!abilityId.isBlank()) {
                                abilities.add(abilityId);
                            }
                        }
                    }

                    ModuleDefinition previous = loaded.put(id,
                            new ModuleDefinition(id, type, Math.max(1, slotCost), requiredSkillNode, Map.copyOf(statModifiers), java.util.List.copyOf(abilities)));
                    if (previous != null) {
                        LOGGER.warn("[Module] Duplicate module id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }
                } catch (RuntimeException ex) {
                    malformed++;
                    LOGGER.warn("[Module] Skipping malformed module definition {} ({}): {}",
                            entry.getKey(), ex.getClass().getSimpleName(), ex.getMessage());
                }
            }

            if (type == ModuleType.ARMOR) {
                ArmorModuleRegistry.replaceAll(loaded);
            } else {
                ToolModuleRegistry.replaceAll(loaded);
            }

            LOGGER.info("[Module] Reloaded {} module definitions: loaded={}, malformed={}",
                    type.name().toLowerCase(Locale.ROOT), loaded.size(), malformed);
        }

        private static String readString(JsonObject root, String primary, String legacy) {
            if (root.has(primary) && root.get(primary).isJsonPrimitive()) {
                return root.get(primary).getAsString();
            }
            if (root.has(legacy) && root.get(legacy).isJsonPrimitive()) {
                return root.get(legacy).getAsString();
            }
            return "";
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

