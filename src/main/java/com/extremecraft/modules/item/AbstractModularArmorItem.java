package com.extremecraft.modules.item;

import com.extremecraft.modules.data.ModuleType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public abstract class AbstractModularArmorItem extends ArmorItem implements IModularItem {
    private final int moduleSlots;

    protected AbstractModularArmorItem(ArmorMaterial material, Type type, int moduleSlots, Properties properties) {
        super(material, type, properties);
        this.moduleSlots = Math.max(1, moduleSlots);
    }

    @Override
    public ModuleType moduleType() {
        return ModuleType.ARMOR;
    }

    @Override
    public int moduleSlots(ItemStack stack) {
        return moduleSlots;
    }
}
