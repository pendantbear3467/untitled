package com.extremecraft.progression.unlock;

import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.classsystem.ClassIdResolver;
import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.progression.stage.StageManager;
import com.extremecraft.skills.SkillsApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class UnlockRuleLoader extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, UnlockRule> RULES = new LinkedHashMap<>();

    public UnlockRuleLoader() {
        super(GSON, "progression/unlocks");
    }

    @SubscribeEvent
    public void onReloadListener(AddReloadListenerEvent event) {
        event.addListener(this);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager manager, ProfilerFiller profiler) {
        Map<String, UnlockRule> loaded = new LinkedHashMap<>();
        int malformed = 0;

        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "unlock_rule");
                String unlock = normalizeId(GsonHelper.getAsString(json, "unlock", entry.getKey().getPath()));
                if (unlock.isBlank()) {
                    LOGGER.warn("[Unlock] Skipping unlock rule with blank id from {}", entry.getKey());
                    continue;
                }

                String requiredClass = ClassIdResolver.normalizeCanonical(normalizeId(readString(json, "required_class", "class")));
                String requiredSkill = normalizeId(readString(json, "required_skill", "skill"));
                int requiredSkillLevel = Math.max(0, readInt(json, "required_skill_level", "skill_level", 0));
                String requiredQuest = normalizeId(readString(json, "required_quest", "quest"));
                String requiredStage = readString(json, "required_stage", "stage").trim();

                UnlockRule previous = loaded.put(unlock,
                        new UnlockRule(unlock, requiredClass, requiredSkill, requiredSkillLevel, requiredQuest, requiredStage));
                if (previous != null) {
                    LOGGER.warn("[Unlock] Duplicate unlock id '{}' detected; keeping latest from {}", unlock, entry.getKey());
                }
            } catch (RuntimeException ex) {
                malformed++;
                LOGGER.warn("[Unlock] Skipping malformed unlock rule {}: {}", entry.getKey(), ex.getMessage());
            }
        }

        if (!map.isEmpty() && loaded.isEmpty()) {
            synchronized (RULES) {
                if (!RULES.isEmpty()) {
                    LOGGER.warn("[Unlock] Reload produced no valid entries; keeping previous rules (malformed={})", malformed);
                    return;
                }
            }
        }

        addBuiltinDefaults(loaded);
        synchronized (RULES) {
            RULES.clear();
            RULES.putAll(loaded);
        }

        LOGGER.info("[Unlock] Reloaded unlock rules: loaded={}, malformed={}", loaded.size(), malformed);
    }

    public static boolean canUnlock(Player player, String unlockId) {
        if (unlockId == null || unlockId.isBlank()) {
            return true;
        }

        UnlockRule rule;
        synchronized (RULES) {
            rule = RULES.get(unlockId.trim().toLowerCase(Locale.ROOT));
        }

        if (rule == null) {
            return true;
        }

        Optional<com.extremecraft.progression.PlayerProgressData> progress = ProgressApi.get(player);

        if (!rule.requiredStage().isBlank()) {
            ProgressionStage required = ProgressionStage.byName(rule.requiredStage()).orElse(ProgressionStage.PRIMITIVE);
            if (!StageManager.hasStage(player, required)) {
                return false;
            }
        }

        if (!rule.requiredClass().isBlank() && progress.isPresent()) {
            String current = progress.get().currentClass();
            if (!ClassIdResolver.matches(rule.requiredClass(), current)) {
                return false;
            }
        }

        if (!rule.requiredSkill().isBlank() && rule.requiredSkillLevel() > 0) {
            int level = SkillsApi.get(player).map(skills -> skills.getSkillLevel(rule.requiredSkill())).orElse(0);
            if (level < rule.requiredSkillLevel()) {
                return false;
            }
        }

        if (!rule.requiredQuest().isBlank() && progress.isPresent()) {
            if (!progress.get().isQuestCompleted(rule.requiredQuest())) {
                return false;
            }
        }

        return true;
    }

    private static void addBuiltinDefaults(Map<String, UnlockRule> loaded) {
        loaded.putIfAbsent("machine:electric_furnace", new UnlockRule("machine:electric_furnace", "", "engineering", 20, "", "ENERGY"));
        loaded.putIfAbsent("machine:enrichment_chamber", new UnlockRule("machine:enrichment_chamber", "", "engineering", 20, "", "ENERGY"));
        loaded.putIfAbsent("machine:rune_infuser", new UnlockRule("machine:rune_infuser", "", "arcane", 15, "", "ADVANCED"));
        loaded.putIfAbsent("machine:quantum_fabricator", new UnlockRule("machine:quantum_fabricator", "engineer", "engineering", 35, "", "ADVANCED"));
        loaded.putIfAbsent("machine:singularity_compressor", new UnlockRule("machine:singularity_compressor", "", "engineering", 50, "", "ENDGAME"));
        loaded.putIfAbsent("recipe:titanium_mining", new UnlockRule("recipe:titanium_mining", "", "mining", 10, "", "INDUSTRIAL"));
    }

    private static String readString(JsonObject json, String primary, String legacy) {
        if (json.has(primary) && json.get(primary).isJsonPrimitive()) {
            return json.get(primary).getAsString();
        }
        if (json.has(legacy) && json.get(legacy).isJsonPrimitive()) {
            return json.get(legacy).getAsString();
        }
        return "";
    }

    private static int readInt(JsonObject json, String primary, String legacy, int fallback) {
        if (json.has(primary) && json.get(primary).isJsonPrimitive() && json.get(primary).getAsJsonPrimitive().isNumber()) {
            return json.get(primary).getAsInt();
        }
        if (json.has(legacy) && json.get(legacy).isJsonPrimitive() && json.get(legacy).getAsJsonPrimitive().isNumber()) {
            return json.get(legacy).getAsInt();
        }
        return fallback;
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            return "";
        }

        String id = raw.trim().toLowerCase(Locale.ROOT);
        if (id.contains("/")) {
            id = id.substring(id.lastIndexOf('/') + 1);
        }
        return id;
    }
}
