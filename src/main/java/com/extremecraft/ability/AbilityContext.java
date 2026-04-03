package com.extremecraft.ability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Immutable execution context propagated through targeting and effect execution.
 *
 * <p>Every transformation returns a new instance, making cast flow explicit and avoiding hidden
 * mutations while multiple systems (target resolver, executor, channel ticker) share state.</p>
 */
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
    /**
     * Builds a baseline context from the caster and optional definition.
     *
     * <p>The default position uses eye height so projectile and ray-based abilities begin from
     * first-person aiming origin.</p>
     */
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

    /**
     * Semantic alias used by older code paths.
     */
    public ServerPlayer caster() {
        return player;
    }

    /**
     * Semantic alias for cast origin used by movement/projectile effects.
     */
    public Vec3 origin() {
        return position;
    }

    /**
     * Returns a cloned context with resolved target entity and center position.
     */
    public AbilityContext withTarget(@Nullable LivingEntity nextTarget, Vec3 nextPosition) {
        return new AbilityContext(player, world, nextTarget, nextPosition, abilityLevel, manaCost, definition, serverTick);
    }

    /**
     * Applies ability level after guarding against invalid level 0/negative values.
     */
    public AbilityContext withAbilityLevel(int nextAbilityLevel) {
        return new AbilityContext(player, world, target, position, Math.max(1, nextAbilityLevel), manaCost, definition, serverTick);
    }

    /**
     * Applies resolved mana cost while ensuring the value is non-negative.
     */
    public AbilityContext withManaCost(int nextManaCost) {
        return new AbilityContext(player, world, target, position, abilityLevel, Math.max(0, nextManaCost), definition, serverTick);
    }
}
