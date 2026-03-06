package com.extremecraft.ability;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AbilityEffect {
    private final String type;
    private final double value;
    private final int duration;
    private final int amplifier;
    private final String id;
    private final Map<String, Double> scalars;

    public AbilityEffect(String type, double value, int duration, int amplifier, String id, Map<String, Double> scalars) {
        this.type = type == null ? "none" : type.trim().toLowerCase();
        this.value = value;
        this.duration = Math.max(0, duration);
        this.amplifier = Math.max(0, amplifier);
        this.id = id == null ? "" : id.trim().toLowerCase();
        this.scalars = Map.copyOf(scalars);
    }

    public String type() {
        return type;
    }

    public double value() {
        return value;
    }

    public int duration() {
        return duration;
    }

    public int amplifier() {
        return amplifier;
    }

    public String id() {
        return id;
    }

    public Map<String, Double> scalars() {
        return scalars;
    }

    public static AbilityEffect fromJson(JsonObject root) {
        String type = GsonHelper.getAsString(root, "type", "none");
        double value = GsonHelper.getAsDouble(root, "value", 0.0D);
        int duration = Math.max(0, GsonHelper.getAsInt(root, "duration", 0));
        int amplifier = Math.max(0, GsonHelper.getAsInt(root, "amplifier", 0));
        String id = GsonHelper.getAsString(root, "id", "");

        Map<String, Double> scalars = new LinkedHashMap<>();
        if (root.has("scalars") && root.get("scalars").isJsonObject()) {
            JsonObject map = root.getAsJsonObject("scalars");
            for (Map.Entry<String, com.google.gson.JsonElement> entry : map.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    scalars.put(entry.getKey().trim().toLowerCase(), entry.getValue().getAsDouble());
                }
            }
        }

        return new AbilityEffect(type, value, duration, amplifier, id, scalars);
    }
}
