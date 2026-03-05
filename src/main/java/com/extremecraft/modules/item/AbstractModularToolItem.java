package com.extremecraft.modules.item;

import com.extremecraft.modules.data.ModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;

public abstract class AbstractModularToolItem extends PickaxeItem implements IModularItem {
    private final int moduleSlots;

    protected AbstractModularToolItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, int moduleSlots, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.moduleSlots = Math.max(1, moduleSlots);
    }

    @Override
    public ModuleType moduleType() {
        return ModuleType.TOOL;
    }

    @Override
    public int moduleSlots(ItemStack stack) {
        return moduleSlots;
    }
}
