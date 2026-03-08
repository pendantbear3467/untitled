package com.extremecraft.magic;

import com.extremecraft.dev.validation.ECTickProfiler;
import com.extremecraft.foundation.ECDestructiveEffectService;
import com.extremecraft.radiation.RadiationService;
import net.minecraft.server.level.ServerLevel;

public final class SpellService {
    private SpellService() {
    }

    public static SpellValidationService.ValidationResult prepareCast(net.minecraft.server.level.ServerPlayer player, Spell spell, SpellCastContext.CastSource source) {
        return SpellValidationService.validate(player, spell, source);
    }

    public static void finishCast(SpellCastContext context, SpellCompiler.CompiledSpell compiled, boolean channelPulse) {
        if (!(context.caster().level() instanceof ServerLevel serverLevel) || compiled == null) {
            return;
        }

        long start = System.nanoTime();
        int aetherCost = channelPulse ? Math.max(1, RitualService.pulseAetherCost(compiled) / 2) : RitualService.pulseAetherCost(compiled);
        if (aetherCost > 0) {
            AetherNetworkService.tryConsume(serverLevel, context.caster().blockPosition(), aetherCost);
        }

        boolean contamination = context.spell().id().contains("dirty_bomb")
                || context.spell().effects().stream().anyMatch(effect -> effect.id().contains("contamination"));
        if (contamination) {
            RadiationService.releaseContamination(serverLevel, context.caster().blockPosition(), Math.max(12.0D, context.spell().radius() * 2.0D), Math.max(3, (int) Math.ceil(context.spell().radius())));
        }

        if (compiled.catastrophic()) {
            ECDestructiveEffectService.queueSphere(
                    serverLevel,
                    context.caster().blockPosition(),
                    compiled.destructiveRadius(),
                    compiled.destructiveBlockBudget(),
                    context.spell().id()
            );
        }

        ECTickProfiler.record("spell_service", System.nanoTime() - start);
    }
}
