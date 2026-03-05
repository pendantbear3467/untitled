package com.extremecraft.item.module;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Module contract for armor-slot effects.
 */
public interface ArmorModule {
    String id();

    void applyArmorEffects(ItemStack stack, int level, Map<String, Float> effectAccumulator);
}
