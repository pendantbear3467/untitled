package com.extremecraft.platform.hook;

import com.extremecraft.ability.Ability;
import com.extremecraft.ability.AbilityDefinition;
import com.extremecraft.ecosystem.core.compat.CoreCompatHooks;
import com.extremecraft.entity.extension.EntityExtension;
import com.extremecraft.machine.MachineDefinition;
import com.extremecraft.machine.MachineRecipe;
import net.minecraft.world.entity.EntityType;

/**
 * Stable extension entry points for runtime integrations.
 */
@Deprecated(forRemoval = false, since = "1.2.0")
public final class ExtremeCraftHooks {
    private ExtremeCraftHooks() {
    }

    public static void registerRuntimeAbility(String id, Ability ability) {
        CoreCompatHooks.registerRuntimeAbility(id, ability);
    }

    public static void registerAbilityDefinition(AbilityDefinition definition) {
        CoreCompatHooks.registerAbilityDefinition(definition);
    }

    public static void registerMachineDefinition(MachineDefinition definition) {
        CoreCompatHooks.registerMachineDefinition(definition);
    }

    public static void registerMachineRecipe(MachineRecipe recipe) {
        CoreCompatHooks.registerMachineRecipe(recipe);
    }

    public static void registerEntityExtension(EntityType<?> entityType, EntityExtension extension) {
        CoreCompatHooks.registerEntityExtension(entityType, extension);
    }
}
