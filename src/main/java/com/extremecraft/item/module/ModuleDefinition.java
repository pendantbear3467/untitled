package com.extremecraft.item.module;

import net.minecraft.world.item.ItemStack;

import java.util.Map;

/**
 * Data-driven module definition loaded from JSON.
 */
public record ModuleDefinition(
        String id,
        ModuleType type,
        int maxLevel,
        Map<String, Float> effects
) implements ArmorModule, ToolModule {

    public boolean supportsArmor() {
        return type == ModuleType.ARMOR || type == ModuleType.UNIVERSAL;
    }

    public boolean supportsTool() {
        return type == ModuleType.TOOL || type == ModuleType.UNIVERSAL;
    }

    @Override
    public void applyArmorEffects(ItemStack stack, int level, Map<String, Float> effectAccumulator) {
        if (!supportsArmor()) {
            return;
        }
        mergeEffects(level, effectAccumulator);
    }

    @Override
    public void applyToolEffects(ItemStack stack, int level, Map<String, Float> effectAccumulator) {
        if (!supportsTool()) {
            return;
        }
        mergeEffects(level, effectAccumulator);
    }

    private void mergeEffects(int level, Map<String, Float> effectAccumulator) {
        int appliedLevel = Math.max(1, Math.min(maxLevel, level));
        for (Map.Entry<String, Float> entry : effects.entrySet()) {
            effectAccumulator.merge(entry.getKey(), entry.getValue() * appliedLevel, Float::sum);
        }
    }
}
