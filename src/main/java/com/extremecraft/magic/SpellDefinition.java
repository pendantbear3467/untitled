package com.extremecraft.magic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class SpellDefinition {
    public enum SpellType {
        PROJECTILE,
        INSTANT,
        AREA,
        CHANNEL,
        SUMMON;

        public static SpellType byName(String value) {
            if (value == null || value.isBlank()) {
                return INSTANT;
            }
            return switch (value.trim().toLowerCase()) {
                case "projectile" -> PROJECTILE;
                case "area", "aoe" -> AREA;
                case "channel", "channeled" -> CHANNEL;
                case "summon" -> SUMMON;
                default -> INSTANT;
            };
        }
    }

    private final String id;
    private final SpellType type;
    private final int manaCost;
    private final int cooldownTicks;
    private final double speed;
    private final double radius;
    private final int channelTicks;
    private final String summonEntity;
    private final List<SpellEffect> effects;
    private final List<SpellModifier> modifiers;

    public SpellDefinition(
            String id,
            SpellType type,
            int manaCost,
            int cooldownTicks,
            double speed,
            double radius,
            int channelTicks,
            String summonEntity,
            List<SpellEffect> effects,
            List<SpellModifier> modifiers
    ) {
        this.id = id;
        this.type = type == null ? SpellType.INSTANT : type;
        this.manaCost = Math.max(0, manaCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.speed = Math.max(0.1D, speed);
        this.radius = Math.max(0.0D, radius);
        this.channelTicks = Math.max(0, channelTicks);
        this.summonEntity = summonEntity == null ? "" : summonEntity.trim().toLowerCase();
        this.effects = List.copyOf(effects);
        this.modifiers = List.copyOf(modifiers);
    }

    public String id() {
        return id;
    }

    public SpellType type() {
        return type;
    }

    public int manaCost() {
        return manaCost;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public double speed() {
        return speed;
    }

    public double radius() {
        return radius;
    }

    public int channelTicks() {
        return channelTicks;
    }

    public String summonEntity() {
        return summonEntity;
    }

    public List<SpellEffect> effects() {
        return effects;
    }

    public List<SpellModifier> modifiers() {
        return modifiers;
    }

    public static SpellDefinition fromJson(ResourceLocation key, JsonObject root) {
        String id = GsonHelper.getAsString(root, "id", key.getPath()).trim().toLowerCase();
        SpellType type = SpellType.byName(GsonHelper.getAsString(root, "type", "instant"));
        int manaCost = Math.max(0, GsonHelper.getAsInt(root, "mana_cost", 0));
        int cooldownTicks;
        if (root.has("cooldown_ticks")) {
            cooldownTicks = Math.max(0, GsonHelper.getAsInt(root, "cooldown_ticks", 0));
        } else {
            cooldownTicks = Math.max(0, GsonHelper.getAsInt(root, "cooldown", 0)) * 20;
        }

        double speed = Math.max(0.1D, GsonHelper.getAsDouble(root, "speed", 1.5D));
        double radius = Math.max(0.0D, GsonHelper.getAsDouble(root, "radius", 0.0D));
        int channelTicks = Math.max(0, GsonHelper.getAsInt(root, "channel_ticks", 60));
        String summonEntity = GsonHelper.getAsString(root, "summon_entity", "");

        List<SpellEffect> effects = new ArrayList<>();
        JsonArray rawEffects = GsonHelper.getAsJsonArray(root, "effects", new JsonArray());
        for (JsonElement element : rawEffects) {
            if (element.isJsonObject()) {
                effects.add(SpellEffect.fromJson(element.getAsJsonObject()));
            }
        }

        List<SpellModifier> modifiers = new ArrayList<>();
        JsonArray rawModifiers = GsonHelper.getAsJsonArray(root, "modifiers", new JsonArray());
        for (JsonElement element : rawModifiers) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject modifier = element.getAsJsonObject();
            String modifierId = GsonHelper.getAsString(modifier, "id", "").trim().toLowerCase();
            double value = GsonHelper.getAsDouble(modifier, "value", 0.0D);
            SpellModifier.Operation operation = SpellModifier.Operation.byName(GsonHelper.getAsString(modifier, "operation", "add"));
            if (!modifierId.isBlank()) {
                modifiers.add(new SpellModifier(modifierId, value, operation));
            }
        }

        return new SpellDefinition(id, type, manaCost, cooldownTicks, speed, radius, channelTicks, summonEntity, effects, modifiers);
    }
}
