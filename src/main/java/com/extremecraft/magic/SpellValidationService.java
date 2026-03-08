package com.extremecraft.magic;

import net.minecraft.server.level.ServerPlayer;

public final class SpellValidationService {
    private SpellValidationService() {
    }

    public static ValidationResult validate(ServerPlayer player, Spell spell, SpellCastContext.CastSource source) {
        if (player == null || spell == null || source == null) {
            return new ValidationResult(false, "missing_context", new SpellCompiler.CompiledSpell("arcane", Spell.SpellForm.INSTANT, 0, false, 0, 0, 0));
        }

        SpellCompiler.CompiledSpell compiled = SpellCompiler.compile(spell);
        if (!RitualService.canCast(player, compiled)) {
            return new ValidationResult(false, "ritual_requirements", compiled);
        }

        if (compiled.aetherCost() > 0 && !AetherNetworkService.canConsume(player.serverLevel(), player.blockPosition(), compiled.aetherCost())) {
            return new ValidationResult(false, "insufficient_aether", compiled);
        }

        return new ValidationResult(true, "ok", compiled);
    }

    public record ValidationResult(boolean allowed, String reason, SpellCompiler.CompiledSpell compiled) {
    }
}
