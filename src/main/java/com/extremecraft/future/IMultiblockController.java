package com.extremecraft.future;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public interface IMultiblockController {
    boolean validateStructure(Level level, BlockPos controllerPos);
}
