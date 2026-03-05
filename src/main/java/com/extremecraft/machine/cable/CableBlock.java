package com.extremecraft.machine.cable;

import com.extremecraft.future.registry.TechBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class CableBlock extends BaseEntityBlock implements EntityBlock {
    private final CableTier tier;

    public CableBlock(CableTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TechBlockEntities.CABLE.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TechBlockEntities.CABLE.get(), CableBlockEntity::serverTick);
    }
}
