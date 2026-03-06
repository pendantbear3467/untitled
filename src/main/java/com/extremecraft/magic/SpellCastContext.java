package com.extremecraft.magic;

import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import net.minecraft.server.level.ServerPlayer;

public record SpellCastContext(
        ServerPlayer caster,
        Spell spell,
        CastSource source,
        double levelMultiplier,
        double skillTreeMultiplier,
        double equipmentMultiplier,
        double magicPowerMultiplier
) {
    public enum CastSource {
        STAFF,
        SPELL_BOOK,
        KEYBIND,
        COMMAND,
        SYSTEM;

        public static CastSource byName(String value) {
            if (value == null || value.isBlank()) {
                return SYSTEM;
            }

            return switch (value.trim().toLowerCase()) {
                case "staff" -> STAFF;
                case "spell_book", "spellbook", "book" -> SPELL_BOOK;
                case "keybind", "hotkey" -> KEYBIND;
                case "command" -> COMMAND;
                default -> SYSTEM;
            };
        }
    }

    public static SpellCastContext create(ServerPlayer caster, Spell spell, CastSource source) {
        PlayerStatsCapability stats = PlayerStatsApi.get(caster).orElse(null);

        double levelMultiplier = 1.0D;
        double skillTreeMultiplier = 1.0D;
        double equipmentMultiplier = 1.0D;
        double magicPowerMultiplier = 1.0D;

        if (stats != null) {
            levelMultiplier = 1.0D + (Math.max(0, stats.level() - 1) * 0.015D);
            skillTreeMultiplier = 1.0D + Math.max(0.0D, stats.spellPowerBonus());
            equipmentMultiplier = 1.0D + Math.max(0.0D, stats.equipmentModifier("spell_power_bonus"));
            magicPowerMultiplier = 1.0D + (Math.max(1, stats.magicPower()) * 0.015D);
        }

        return new SpellCastContext(caster, spell, source, levelMultiplier, skillTreeMultiplier, equipmentMultiplier, magicPowerMultiplier);
    }

    public double totalPowerMultiplier() {
        return Math.max(0.1D, levelMultiplier * skillTreeMultiplier * equipmentMultiplier * magicPowerMultiplier);
    }

    public float scaledDamage(double baseDamage) {
        return (float) Math.max(0.0D, baseDamage * totalPowerMultiplier());
    }

    public int scaledDurationTicks(int baseDurationTicks) {
        if (baseDurationTicks <= 0) {
            return 0;
        }

        double durationScale = 1.0D + ((totalPowerMultiplier() - 1.0D) * 0.35D);
        return Math.max(20, (int) Math.round(baseDurationTicks * durationScale));
    }

    public double scaledRadius(double baseRadius) {
        if (baseRadius <= 0.0D) {
            return 0.0D;
        }

        double radiusScale = 1.0D + ((totalPowerMultiplier() - 1.0D) * 0.20D);
        return Math.max(1.0D, baseRadius * radiusScale);
    }

    public int scaledManaCost() {
        return Math.max(0, spell.manaCost());
    }
}
