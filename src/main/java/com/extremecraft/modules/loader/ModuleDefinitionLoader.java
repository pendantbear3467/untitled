package com.extremecraft.modules.loader;

import com.extremecraft.modules.data.ModuleDefinition;
import com.extremecraft.modules.data.ModuleType;
import com.extremecraft.modules.registry.ArmorModuleRegistry;
import com.extremecraft.modules.registry.ToolModuleRegistry;
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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ModuleDefinitionLoader {
    private static final Gson GSON = new Gson();

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

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject root = entry.getValue().getAsJsonObject();
                String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase();
                if (id.contains("/")) {
                    id = id.substring(id.lastIndexOf('/') + 1);
                }
                if (id.isBlank()) {
                    continue;
                }

                int slotCost = Math.max(1, GsonHelper.getAsInt(root, "slot_cost", 1));
                String requiredSkillNode = GsonHelper.getAsString(root, "required_skill_node", "").trim().toLowerCase();

                Map<String, Float> statModifiers = new LinkedHashMap<>();
                if (root.has("stat_modifiers") && root.get("stat_modifiers").isJsonObject()) {
                    JsonObject statJson = root.getAsJsonObject("stat_modifiers");
                    for (Map.Entry<String, JsonElement> statEntry : statJson.entrySet()) {
                        if (statEntry.getValue().isJsonPrimitive() && statEntry.getValue().getAsJsonPrimitive().isNumber()) {
                            statModifiers.put(statEntry.getKey().trim().toLowerCase(), statEntry.getValue().getAsFloat());
                        }
                    }
                }

                Set<String> abilities = new LinkedHashSet<>();
                if (root.has("abilities") && root.get("abilities").isJsonArray()) {
                    for (JsonElement ability : root.getAsJsonArray("abilities")) {
                        String abilityId = ability.getAsString().trim().toLowerCase();
                        if (!abilityId.isBlank()) {
                            abilities.add(abilityId);
                        }
                    }
                }

                loaded.put(id, new ModuleDefinition(id, type, slotCost, requiredSkillNode, Map.copyOf(statModifiers), java.util.List.copyOf(abilities)));
            }

            if (type == ModuleType.ARMOR) {
                ArmorModuleRegistry.replaceAll(loaded);
            } else {
                ToolModuleRegistry.replaceAll(loaded);
            }
        }
    }
}
