package com.extremecraft.ability;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class AbilityDefinition {
    public enum TargetType {
        SELF,
        ENTITY,
        AREA,
        PROJECTILE,
        NONE;

        public static TargetType byName(String value) {
            if (value == null || value.isBlank()) {
                return SELF;
            }

            return switch (value.trim().toLowerCase()) {
                case "self" -> SELF;
                case "entity", "single" -> ENTITY;
                case "area", "aoe" -> AREA;
                case "projectile" -> PROJECTILE;
                default -> NONE;
            };
        }
    }

    private final String id;
    private final int manaCost;
    private final int cooldownTicks;
    private final TargetType targetType;
    private final double radius;
    private final double range;
    private final String requiredClass;
    private final List<AbilityEffect> effects;

    public AbilityDefinition(
            String id,
            int manaCost,
            int cooldownTicks,
            TargetType targetType,
            double radius,
            double range,
            String requiredClass,
            List<AbilityEffect> effects
    ) {
        this.id = id;
        this.manaCost = Math.max(0, manaCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.targetType = targetType == null ? TargetType.SELF : targetType;
        this.radius = Math.max(0.0D, radius);
        this.range = Math.max(1.0D, range);
        this.requiredClass = requiredClass == null ? "" : requiredClass.trim().toLowerCase();
        this.effects = List.copyOf(effects);
    }

    public String id() {
        return id;
    }

    public int manaCost() {
        return manaCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public TargetType targetType() {
        return targetType;
    }

    public double radius() {
        return radius;
    }

    public double range() {
        return range;
    }

    public String requiredClass() {
        return requiredClass;
    }

    public List<AbilityEffect> effects() {
        return effects;
    }

    public static AbilityDefinition fromJson(ResourceLocation key, JsonObject root) {
        String id = GsonHelper.getAsString(root, "id", key.getPath()).trim().toLowerCase();
        int manaCost = Math.max(0, GsonHelper.getAsInt(root, "mana_cost", 0));

        int cooldownTicks;
        if (root.has("cooldown_ticks")) {
            cooldownTicks = Math.max(0, GsonHelper.getAsInt(root, "cooldown_ticks", 0));
        } else {
            cooldownTicks = Math.max(0, GsonHelper.getAsInt(root, "cooldown", 0)) * 20;
        }

        TargetType targetType = TargetType.byName(GsonHelper.getAsString(root, "target", "self"));
        double radius = Math.max(0.0D, GsonHelper.getAsDouble(root, "radius", 0.0D));
        double range = Math.max(1.0D, GsonHelper.getAsDouble(root, "range", 16.0D));
        String requiredClass = GsonHelper.getAsString(root, "required_class", "");

        List<AbilityEffect> effects = new ArrayList<>();
        JsonArray rawEffects = GsonHelper.getAsJsonArray(root, "effects", new JsonArray());
        for (JsonElement element : rawEffects) {
            if (element.isJsonObject()) {
                effects.add(AbilityEffect.fromJson(element.getAsJsonObject()));
            }
        }

        return new AbilityDefinition(id, manaCost, cooldownTicks, targetType, radius, range, requiredClass, effects);
    }
}
