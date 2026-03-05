package com.extremecraft.modules.item;

import net.minecraft.world.item.ArmorItem;

public class PioneerArmorItem extends AbstractModularArmorItem {
    public PioneerArmorItem(ArmorItem.Type type, int moduleSlots, Properties properties) {
        super(ModularArmorMaterial.PIONEER, type, moduleSlots, properties);
    }
}
