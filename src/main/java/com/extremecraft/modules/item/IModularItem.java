package com.extremecraft.modules.item;

import com.extremecraft.modules.data.ModuleType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IModularItem {
    ModuleType moduleType();

    int moduleSlots(ItemStack stack);

    default List<String> installedModules(ItemStack stack) {
        return ModuleStackData.readModules(stack);
    }

    default boolean installModule(ItemStack stack, String moduleId) {
        return ModuleStackData.installModule(stack, moduleId, moduleSlots(stack));
    }
}
