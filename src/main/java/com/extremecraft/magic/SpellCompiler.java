package com.extremecraft.magic;

import com.extremecraft.config.ECFoundationConfig;

public final class SpellCompiler {
    private SpellCompiler() {
    }

    public static CompiledSpell compile(Spell spell) {
        if (spell == null) {
            return new CompiledSpell("", Spell.SpellForm.INSTANT, 0, false, 0, 0, 0);
        }

        String school = normalizeSchool(spell.school().isBlank() ? spell.element() : spell.school());
        Spell.SpellForm form = spell.form();
        int aetherCost = Math.max(0, AetherNetworkService.spellAetherCost(spell));
        boolean catastrophic = spell.catastrophic()
                || spell.id().contains("nuke")
                || spell.id().contains("horizon")
                || spell.id().contains("singularity");
        int destructiveRadius = Math.min(
                ECFoundationConfig.catastrophicMaxRadius(),
                Math.max(0, spell.destructiveRadius() > 0 ? spell.destructiveRadius() : (catastrophic ? (int) Math.ceil(spell.radius()) : 0))
        );
        int destructiveBudget = Math.min(
                ECFoundationConfig.catastrophicMaxAffectedBlocks(),
                Math.max(0, spell.destructiveBlockBudget() > 0 ? spell.destructiveBlockBudget() : (catastrophic ? 64 : 0))
        );
        int ritualTicks = Math.max(spell.ritualTicks(), form == Spell.SpellForm.RITUAL ? 60 : 0);
        return new CompiledSpell(school, form, aetherCost, catastrophic, destructiveRadius, destructiveBudget, ritualTicks);
    }

    private static String normalizeSchool(String school) {
        String normalized = school == null ? "" : school.trim().toLowerCase();
        if (normalized.isBlank()) {
            return "arcane";
        }
        return switch (normalized) {
            case "water", "ice", "water_ice" -> "water_ice";
            default -> normalized;
        };
    }

    public record CompiledSpell(
            String school,
            Spell.SpellForm form,
            int aetherCost,
            boolean catastrophic,
            int destructiveRadius,
            int destructiveBlockBudget,
            int ritualTicks
    ) {
    }
}
