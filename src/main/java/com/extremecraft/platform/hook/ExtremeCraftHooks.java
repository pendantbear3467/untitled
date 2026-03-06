package com.extremecraft.platform.hook;

import com.extremecraft.ability.Ability;
import com.extremecraft.ability.AbilityDefinition;
import com.extremecraft.ability.AbilityRegistry;
import com.extremecraft.entity.extension.EntityExtension;
import com.extremecraft.entity.extension.EntityExtensionRegistry;
import com.extremecraft.machine.MachineDefinition;
import com.extremecraft.machine.MachineRecipe;
import com.extremecraft.machine.MachineRegistry;
import net.minecraft.world.entity.EntityType;

/**
 * Stable extension entry points for runtime integrations.
 */
public final class ExtremeCraftHooks {
    private ExtremeCraftHooks() {
    }

    public static void registerRuntimeAbility(String id, Ability ability) {
        AbilityRegistry.register(id, ability);
    }

    public static void registerAbilityDefinition(AbilityDefinition definition) {
        AbilityRegistry.registerDefinition(definition);
    }

    public static void registerMachineDefinition(MachineDefinition definition) {
        MachineRegistry.registerMachineDefinition(definition);
    }

    public static void registerMachineRecipe(MachineRecipe recipe) {
        MachineRegistry.registerRecipeDefinition(recipe);
    }

    public static void registerEntityExtension(EntityType<?> entityType, EntityExtension extension) {
        EntityExtensionRegistry.register(entityType, extension);
    }
}
