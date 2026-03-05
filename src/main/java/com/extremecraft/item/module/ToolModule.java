package com.extremecraft.item.module;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Module contract for tool-in-hand effects.
 */
public interface ToolModule {
    String id();

    void applyToolEffects(ItemStack stack, int level, Map<String, Float> effectAccumulator);
}
