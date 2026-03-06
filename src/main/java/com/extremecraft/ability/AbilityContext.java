package com.extremecraft.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public record AbilityContext(
        ServerPlayer player,
        ServerLevel world,
        @Nullable LivingEntity target,
        Vec3 position,
        int abilityLevel,
        int manaCost,
        @Nullable AbilityDefinition definition,
        long serverTick
) {
    public static AbilityContext of(ServerPlayer caster, @Nullable AbilityDefinition definition) {
        return new AbilityContext(
                caster,
                caster.serverLevel(),
                null,
                caster.position().add(0.0D, caster.getEyeHeight(), 0.0D),
                1,
                definition == null ? 0 : definition.manaCost(),
                definition,
                caster.level().getGameTime()
        );
    }

    public ServerPlayer caster() {
        return player;
    }

    public Vec3 origin() {
        return position;
    }

    public AbilityContext withTarget(@Nullable LivingEntity nextTarget, Vec3 nextPosition) {
        return new AbilityContext(player, world, nextTarget, nextPosition, abilityLevel, manaCost, definition, serverTick);
    }

    public AbilityContext withAbilityLevel(int nextAbilityLevel) {
        return new AbilityContext(player, world, target, position, Math.max(1, nextAbilityLevel), manaCost, definition, serverTick);
    }

    public AbilityContext withManaCost(int nextManaCost) {
        return new AbilityContext(player, world, target, position, abilityLevel, Math.max(0, nextManaCost), definition, serverTick);
    }
}
