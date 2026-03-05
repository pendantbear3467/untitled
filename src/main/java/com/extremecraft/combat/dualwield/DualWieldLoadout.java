package com.extremecraft.combat.dualwield;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * Stores one hand pair for fast weapon/tool set swaps.
 */
public class DualWieldLoadout {
    private ItemStack mainHandItem = ItemStack.EMPTY;
    private ItemStack offHandItem = ItemStack.EMPTY;

    public ItemStack mainHandItem() {
        return mainHandItem;
    }

    public ItemStack offHandItem() {
        return offHandItem;
    }

    public void set(ItemStack main, ItemStack off) {
        this.mainHandItem = main.copy();
        this.offHandItem = off.copy();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.put("main", mainHandItem.save(new CompoundTag()));
        tag.put("off", offHandItem.save(new CompoundTag()));
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        this.mainHandItem = ItemStack.of(tag.getCompound("main"));
        this.offHandItem = ItemStack.of(tag.getCompound("off"));
    }
}
