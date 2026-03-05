package com.extremecraft.skills;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SkillRegistry extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, SkillDefinition> SKILLS = new LinkedHashMap<>();

    public SkillRegistry() {
        super(GSON, "skills");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        SKILLS.clear();

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "skill_definition");
            String id = GsonHelper.getAsString(json, "skill", entry.getKey().getPath());
            int maxLevel = Math.max(1, GsonHelper.getAsInt(json, "max_level", 1));
            double bonusPerLevel = Math.max(0.0D, GsonHelper.getAsDouble(json, "bonus_per_level", 0.0D));

            SKILLS.put(id, new SkillDefinition(id, maxLevel, bonusPerLevel));
        }
    }

    public static SkillDefinition byId(String id) {
        return SKILLS.get(id);
    }

    public static Collection<SkillDefinition> all() {
        return SKILLS.values();
    }
}
