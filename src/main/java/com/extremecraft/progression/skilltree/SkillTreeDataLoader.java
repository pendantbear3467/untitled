package com.extremecraft.progression.skilltree;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SkillTreeDataLoader {
    private static final Gson GSON = new Gson();

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(GSON, "skilltrees");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, SkillTree> parsedTrees = new LinkedHashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }

                JsonObject root = entry.getValue().getAsJsonObject();
                String treeId = entry.getKey().getPath();
                if (treeId.contains("/")) {
                    treeId = treeId.substring(treeId.lastIndexOf('/') + 1);
                }

                SkillTree tree = new SkillTree(treeId);
                JsonArray nodes = root.has("nodes") && root.get("nodes").isJsonArray()
                        ? root.getAsJsonArray("nodes")
                        : new JsonArray();

                for (JsonElement nodeElement : nodes) {
                    if (!nodeElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject nodeObj = nodeElement.getAsJsonObject();
                    String id = nodeObj.has("id") ? nodeObj.get("id").getAsString() : "";
                    if (id.isBlank()) {
                        continue;
                    }

                    int x = nodeObj.has("x") ? nodeObj.get("x").getAsInt() : 0;
                    int y = nodeObj.has("y") ? nodeObj.get("y").getAsInt() : 0;
                    int cost = Math.max(1, nodeObj.has("cost") ? nodeObj.get("cost").getAsInt() : 1);

                    List<String> requires = new ArrayList<>();
                    if (nodeObj.has("requires") && nodeObj.get("requires").isJsonArray()) {
                        for (JsonElement req : nodeObj.getAsJsonArray("requires")) {
                            requires.add(req.getAsString());
                        }
                    }

                    Map<String, Double> modifiers = new HashMap<>();
                    if (nodeObj.has("statModifiers") && nodeObj.get("statModifiers").isJsonObject()) {
                        JsonObject mods = nodeObj.getAsJsonObject("statModifiers");
                        for (Map.Entry<String, JsonElement> modEntry : mods.entrySet()) {
                            modifiers.put(modEntry.getKey(), modEntry.getValue().getAsDouble());
                        }
                    }

                    String bonus = nodeObj.has("bonus") ? nodeObj.get("bonus").getAsString() : "";
                    tree.addNode(new SkillNode(id, x, y, cost, requires, modifiers, bonus));
                }

                if (!tree.isEmpty()) {
                    parsedTrees.put(tree.id(), tree);
                }
            }
            SkillTreeRegistry.replaceAll(parsedTrees);
        }
    }
}
