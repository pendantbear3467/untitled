package com.extremecraft.progression.skilltree;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class SkillTreeDataLoader {
    private static final Gson GSON = new Gson();

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    private static class Loader extends SimpleJsonResourceReloadListener {
        Loader() {
            super(GSON, "skill_trees");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            SkillTreeManager.loadFromJson(jsonMap);

            // Keep legacy registry synchronized for older screens/services.
            Map<String, SkillTree> legacyTrees = new LinkedHashMap<>();
            for (Map.Entry<String, java.util.List<SkillNode>> entry : SkillTreeManager.allTrees().entrySet()) {
                SkillTree tree = new SkillTree(entry.getKey());
                for (SkillNode node : entry.getValue()) {
                    tree.addNode(node);
                }
                legacyTrees.put(tree.id(), tree);
            }
            SkillTreeRegistry.replaceAll(legacyTrees);
        }
    }
}
