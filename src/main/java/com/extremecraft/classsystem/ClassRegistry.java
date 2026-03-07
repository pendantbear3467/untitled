package com.extremecraft.classsystem;

import com.extremecraft.api.ExtremeCraftAPI;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ClassRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Map<String, PlayerClass> DEFINITIONS = new LinkedHashMap<>();
    private static List<PlayerClass> CACHE = List.of();

    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new Loader());
    }

    public static synchronized PlayerClass get(String id) {
        return id == null ? null : DEFINITIONS.get(id.trim().toLowerCase(Locale.ROOT));
    }

    public static synchronized Collection<PlayerClass> all() {
        return CACHE;
    }

    public static synchronized int size() {
        return DEFINITIONS.size();
    }

    private static synchronized void replaceAll(Map<String, PlayerClass> loaded) {
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
        CACHE = List.copyOf(DEFINITIONS.values());
    }

    private static final class Loader extends SimpleJsonResourceReloadListener {
        private static final Gson GSON = new Gson();

        private Loader() {
            super(GSON, "classes");
        }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {
            Map<String, PlayerClass> loaded = new LinkedHashMap<>();

            for (Map.Entry<ResourceLocation, JsonElement> entry : jsonMap.entrySet()) {
                try {
                    if (!entry.getValue().isJsonObject()) {
                        continue;
                    }

                    JsonObject root = entry.getValue().getAsJsonObject();
                    String id = GsonHelper.getAsString(root, "id", entry.getKey().getPath()).trim().toLowerCase(Locale.ROOT);
                    if (id.isBlank()) {
                        LOGGER.warn("[Class] Skipping class with blank id from {}", entry.getKey());
                        continue;
                    }

                    Map<String, Double> statScaling = readNumberMap(root, "stat_scaling", 1.0D);
                    Map<String, Double> combatModifiers = readNumberMap(root, "combat_modifiers", 0.0D);
                    Map<String, Double> passives = readNumberMap(root, "passive_bonuses", 0.0D);
                    double manaRegenModifier = GsonHelper.getAsDouble(root, "mana_regen_modifier", passives.getOrDefault("mana_regen", 0.0D));

                    List<String> abilityAccess = readStringList(root, "active_abilities");
                    List<String> spellAccess = readStringList(root, "spells");

                    int requiredLevel = 1;
                    if (root.has("requirements") && root.get("requirements").isJsonObject()) {
                        requiredLevel = Math.max(1, GsonHelper.getAsInt(root.getAsJsonObject("requirements"), "level", 1));
                    }

                    PlayerClass previous = loaded.put(id, new PlayerClass(
                            id,
                            statScaling,
                            manaRegenModifier,
                            combatModifiers,
                            passives,
                            abilityAccess,
                            spellAccess,
                            requiredLevel
                    ));
                    if (previous != null) {
                        LOGGER.warn("[Class] Duplicate class id '{}' detected; keeping latest from {}", id, entry.getKey());
                    }
                } catch (RuntimeException ex) {
                    LOGGER.warn("[Class] Skipping malformed class definition {}: {}", entry.getKey(), ex.getMessage());
                }
            }

            ExtremeCraftAPI.classes().forEach(klass -> loaded.putIfAbsent(klass.id(),
                    new PlayerClass(klass.id(), Map.of(), 0.0D, Map.of(), Map.of(), List.of(), List.of(), 1)));

            loaded.putIfAbsent("warrior", new PlayerClass("warrior", Map.of("strength", 1.2D), 0.0D,
                    Map.of("melee_damage", 2.0D), Map.of(), List.of(), List.of(), 1));
            loaded.putIfAbsent("mage", new PlayerClass("mage", Map.of("intelligence", 1.25D), 0.08D,
                    Map.of("spell_power", 0.25D), Map.of("mana_regen", 0.08D), List.of(), List.of(), 1));
            loaded.putIfAbsent("engineer", new PlayerClass("engineer", Map.of("agility", 1.1D), 0.02D,
                    Map.of("attack_speed", 0.15D), Map.of(), List.of(), List.of(), 1));
            loaded.putIfAbsent("ranger", new PlayerClass("ranger", Map.of("agility", 1.2D), 0.03D,
                    Map.of("projectile_damage", 0.20D), Map.of(), List.of(), List.of(), 1));

            replaceAll(loaded);
        }

        private static Map<String, Double> readNumberMap(JsonObject root, String key, double fallbackValue) {
            Map<String, Double> map = new LinkedHashMap<>();
            if (!root.has(key) || !root.get(key).isJsonObject()) {
                return Map.copyOf(map);
            }

            JsonObject section = root.getAsJsonObject(key);
            for (Map.Entry<String, JsonElement> entry : section.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    map.put(entry.getKey().trim().toLowerCase(Locale.ROOT), entry.getValue().getAsDouble());
                }
            }

            if (map.isEmpty() && fallbackValue != 0.0D) {
                map.put(key, fallbackValue);
            }

            return Map.copyOf(map);
        }

        private static List<String> readStringList(JsonObject root, String key) {
            List<String> list = new ArrayList<>();
            JsonArray json = GsonHelper.getAsJsonArray(root, key, new JsonArray());
            for (JsonElement entry : json) {
                String value = entry.getAsString().trim().toLowerCase(Locale.ROOT);
                if (!value.isBlank()) {
                    list.add(value);
                }
            }
            return List.copyOf(list);
        }
    }
}
