package com.extremecraft.future;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public interface IAutomationTransport {
    boolean tryInsertItem(BlockPos from, BlockPos to, ItemStack stack);
}
