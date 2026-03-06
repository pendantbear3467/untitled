package com.extremecraft.magic;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public record SpellEffect(String type, double value, int duration, int amplifier, String id) {
    public static SpellEffect fromJson(JsonObject json) {
        String type = GsonHelper.getAsString(json, "type", "none").trim().toLowerCase();
        double value = GsonHelper.getAsDouble(json, "value", 0.0D);
        int duration = Math.max(0, GsonHelper.getAsInt(json, "duration", 0));
        int amplifier = Math.max(0, GsonHelper.getAsInt(json, "amplifier", 0));
        String id = GsonHelper.getAsString(json, "id", "").trim().toLowerCase();
        return new SpellEffect(type, value, duration, amplifier, id);
    }
}
