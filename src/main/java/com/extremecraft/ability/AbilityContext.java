package com.extremecraft.ability;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public record AbilityContext(ServerPlayer caster, AbilityDefinition definition, Vec3 origin, long serverTick) {
    public static AbilityContext of(ServerPlayer caster, AbilityDefinition definition) {
        return new AbilityContext(caster, definition, caster.position().add(0.0D, caster.getEyeHeight(), 0.0D), caster.level().getGameTime());
    }
}
