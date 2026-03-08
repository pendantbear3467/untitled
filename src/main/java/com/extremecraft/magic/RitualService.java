package com.extremecraft.magic;

import net.minecraft.server.level.ServerPlayer;

public final class RitualService {
    private RitualService() {
    }

    public static boolean canCast(ServerPlayer player, SpellCompiler.CompiledSpell compiled) {
        if (player == null || compiled == null) {
            return false;
        }

        if (compiled.form() == Spell.SpellForm.RITUAL) {
            return player.onGround();
        }

        return true;
    }

    public static int pulseAetherCost(SpellCompiler.CompiledSpell compiled) {
        if (compiled == null || compiled.aetherCost() <= 0) {
            return 0;
        }
        if (compiled.form() == Spell.SpellForm.RITUAL || compiled.form() == Spell.SpellForm.SIGIL) {
            return Math.max(1, compiled.aetherCost() / 2);
        }
        return compiled.aetherCost();
    }
}
