package com.extremecraft.magic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;

public final class Spell {
    public enum SpellType {
        PROJECTILE,
        AREA,
        BUFF,
        DEBUFF,
        SUMMON,
        BEAM,
        CHANNEL,
        INSTANT;

        public static SpellType byName(String value) {
            if (value == null || value.isBlank()) {
                return INSTANT;
            }

            return switch (value.trim().toLowerCase()) {
                case "projectile" -> PROJECTILE;
                case "area", "aoe" -> AREA;
                case "buff" -> BUFF;
                case "debuff" -> DEBUFF;
                case "summon" -> SUMMON;
                case "beam", "ray" -> BEAM;
                case "channel", "channeled" -> CHANNEL;
                default -> INSTANT;
            };
        }
    }

    private final String id;
    private final SpellType type;
    private final int manaCost;
    private final int cooldownTicks;
    private final double damage;
    private final double speed;
    private final double radius;
    private final double range;
    private final int durationTicks;
    private final int channelTicks;
    private final String element;
    private final String particle;
    private final String sound;
    private final String summonEntity;
    private final List<SpellEffect> effects;
    private final List<SpellModifier> modifiers;

    public Spell(
            String id,
            SpellType type,
            int manaCost,
            int cooldownTicks,
            double damage,
            double speed,
            double radius,
            double range,
            int durationTicks,
            int channelTicks,
            String element,
            String particle,
            String sound,
            String summonEntity,
            List<SpellEffect> effects,
            List<SpellModifier> modifiers
    ) {
        this.id = id == null ? "" : id.trim().toLowerCase();
        this.type = type == null ? SpellType.INSTANT : type;
        this.manaCost = Math.max(0, manaCost);
        this.cooldownTicks = Math.max(0, cooldownTicks);
        this.damage = Math.max(0.0D, damage);
        this.speed = Math.max(0.1D, speed);
        this.radius = Math.max(0.0D, radius);
        this.range = Math.max(1.0D, range);
        this.durationTicks = Math.max(0, durationTicks);
        this.channelTicks = Math.max(0, channelTicks);
        this.element = element == null ? "" : element.trim().toLowerCase();
        this.particle = particle == null ? "" : particle.trim().toLowerCase();
        this.sound = sound == null ? "" : sound.trim().toLowerCase();
        this.summonEntity = summonEntity == null ? "" : summonEntity.trim().toLowerCase();
        this.effects = effects == null ? List.of() : List.copyOf(effects);
        this.modifiers = modifiers == null ? List.of() : List.copyOf(modifiers);
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

    public double damage() {
        return damage;
    }

    public double speed() {
        return speed;
    }

    public double radius() {
        return radius;
    }

    public double range() {
        return range;
    }

    public int durationTicks() {
        return durationTicks;
    }

    public int channelTicks() {
        return channelTicks;
    }

    public String element() {
        return element;
    }

    public String particle() {
        return particle;
    }

    public String sound() {
        return sound;
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

    public static Spell fromJson(ResourceLocation key, JsonObject root) {
        String id = GsonHelper.getAsString(root, "id", key.getPath()).trim().toLowerCase();
        SpellType type = SpellType.byName(GsonHelper.getAsString(root, "type", "instant"));

        int manaCost = Math.max(0, GsonHelper.getAsInt(root, "mana_cost", 0));
        int cooldownTicks = readTicks(root, "cooldown_ticks", "cooldown", 0);

        double speed = Math.max(0.1D, GsonHelper.getAsDouble(root, "speed", 1.5D));
        double radius = Math.max(0.0D, GsonHelper.getAsDouble(root, "radius", 4.0D));
        double range = Math.max(1.0D, GsonHelper.getAsDouble(root, "range", 24.0D));
        int durationTicks = readTicks(root, "duration_ticks", "duration", 4);
        int channelTicks = readTicks(root, "channel_ticks", "channel_duration", 3);

        String element = GsonHelper.getAsString(root, "element", "");
        String particle = GsonHelper.getAsString(root, "particle", "");
        String sound = GsonHelper.getAsString(root, "sound", "");
        String summonEntity = GsonHelper.getAsString(root, "summon_entity", "");

        List<SpellEffect> effects = new ArrayList<>();
        JsonArray rawEffects = GsonHelper.getAsJsonArray(root, "effects", new JsonArray());
        for (JsonElement elementEntry : rawEffects) {
            if (elementEntry.isJsonObject()) {
                effects.add(SpellEffect.fromJson(elementEntry.getAsJsonObject()));
            }
        }

        List<SpellModifier> modifiers = new ArrayList<>();
        JsonArray rawModifiers = GsonHelper.getAsJsonArray(root, "modifiers", new JsonArray());
        for (JsonElement elementEntry : rawModifiers) {
            if (!elementEntry.isJsonObject()) {
                continue;
            }

            JsonObject modifier = elementEntry.getAsJsonObject();
            String modifierId = GsonHelper.getAsString(modifier, "id", "").trim().toLowerCase();
            if (modifierId.isBlank()) {
                continue;
            }

            double value = GsonHelper.getAsDouble(modifier, "value", 0.0D);
            SpellModifier.Operation operation = SpellModifier.Operation.byName(GsonHelper.getAsString(modifier, "operation", "add"));
            modifiers.add(new SpellModifier(modifierId, value, operation));
        }

        double damage = Math.max(0.0D, GsonHelper.getAsDouble(root, "damage", 0.0D));
        if (damage <= 0.0D) {
            for (SpellEffect effect : effects) {
                if ("damage".equals(effect.type())) {
                    damage = Math.max(0.0D, effect.value());
                    break;
                }
            }
        }

        return new Spell(id, type, manaCost, cooldownTicks, damage, speed, radius, range, durationTicks, channelTicks,
                element, particle, sound, summonEntity, effects, modifiers);
    }

    private static int readTicks(JsonObject root, String ticksKey, String secondsKey, int fallbackSeconds) {
        if (root.has(ticksKey)) {
            return Math.max(0, GsonHelper.getAsInt(root, ticksKey, 0));
        }
        return Math.max(0, GsonHelper.getAsInt(root, secondsKey, fallbackSeconds)) * 20;
    }
}
