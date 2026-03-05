package com.extremecraft.modules.item;

import com.extremecraft.modules.data.ModuleType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

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

    @Override
    public void onArmorTick(ItemStack stack, Level level, Player player) {
        if (!level.isClientSide && player.tickCount % 20 == 0) {
            com.extremecraft.modules.runtime.ModuleRuntimeService.applyPassiveModules(player, stack);
        }
    }
}
