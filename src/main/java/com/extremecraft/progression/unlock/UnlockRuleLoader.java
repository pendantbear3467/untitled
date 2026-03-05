package com.extremecraft.progression.unlock;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.progression.stage.StageManager;
import com.extremecraft.skills.SkillsApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

public class UnlockRuleLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, UnlockRule> RULES = new HashMap<>();

    public UnlockRuleLoader() {
        super(GSON, "progression/unlocks");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        RULES.clear();

        map.forEach((id, element) -> {
            JsonObject json = GsonHelper.convertToJsonObject(element, "unlock_rule");
            String unlock = GsonHelper.getAsString(json, "unlock", id.getPath());
            String requiredClass = GsonHelper.getAsString(json, "required_class", "");
            String requiredSkill = GsonHelper.getAsString(json, "required_skill", "");
            int requiredSkillLevel = Math.max(0, GsonHelper.getAsInt(json, "required_skill_level", 0));
            String requiredQuest = GsonHelper.getAsString(json, "required_quest", "");
            String requiredStage = GsonHelper.getAsString(json, "required_stage", "");
            RULES.put(unlock, new UnlockRule(unlock, requiredClass, requiredSkill, requiredSkillLevel, requiredQuest, requiredStage));
        });

        RULES.putIfAbsent("machine:electric_furnace", new UnlockRule("machine:electric_furnace", "", "engineering", 20, "", "ENERGY"));
        RULES.putIfAbsent("machine:enrichment_chamber", new UnlockRule("machine:enrichment_chamber", "", "engineering", 20, "", "ENERGY"));
        RULES.putIfAbsent("machine:rune_infuser", new UnlockRule("machine:rune_infuser", "", "arcane", 15, "", "ADVANCED"));
        RULES.putIfAbsent("machine:quantum_fabricator", new UnlockRule("machine:quantum_fabricator", "engineer", "engineering", 35, "", "ADVANCED"));
        RULES.putIfAbsent("machine:singularity_compressor", new UnlockRule("machine:singularity_compressor", "", "engineering", 50, "", "ENDGAME"));
        RULES.putIfAbsent("recipe:titanium_mining", new UnlockRule("recipe:titanium_mining", "", "mining", 10, "", "INDUSTRIAL"));
    }

    public static boolean canUnlock(Player player, String unlockId) {
        UnlockRule rule = RULES.get(unlockId);
        if (rule == null) {
            return true;
        }

        if (!rule.requiredStage().isBlank()) {
            ProgressionStage required = ProgressionStage.byName(rule.requiredStage()).orElse(ProgressionStage.PRIMITIVE);
            if (!StageManager.hasStage(player, required)) {
                return false;
            }
        }

        if (!rule.requiredClass().isBlank() && ProgressApi.get(player).isPresent()) {
            String current = ProgressApi.get(player).get().currentClass();
            if (!rule.requiredClass().equalsIgnoreCase(current)) {
                return false;
            }
        }

        if (!rule.requiredSkill().isBlank() && rule.requiredSkillLevel() > 0) {
            int level = SkillsApi.get(player).map(skills -> skills.getSkillLevel(rule.requiredSkill())).orElse(0);
            if (level < rule.requiredSkillLevel()) {
                return false;
            }
        }

        if (!rule.requiredQuest().isBlank() && ProgressApi.get(player).isPresent()) {
            if (!ProgressApi.get(player).get().isQuestCompleted(rule.requiredQuest())) {
                return false;
            }
        }

        return true;
    }
}
