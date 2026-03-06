package com.extremecraft.machine.core;

import com.extremecraft.config.Config;
import com.extremecraft.future.registry.TechBlockEntities;
import com.extremecraft.progression.ProgressionGate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

public class MachineBlock extends BaseEntityBlock implements EntityBlock {
    public MachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return TechBlockEntities.TECH_MACHINE.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TechBlockEntities.TECH_MACHINE.get(), TechMachineBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof TechMachineBlockEntity machine)) {
            return InteractionResult.FAIL;
        }

        String machineId = machine.getMachineId();
        if (!Config.isMachineEnabled(machineId)) {
            player.displayClientMessage(Component.translatable("message.extremecraft.machine_disabled", machineId), true);
            return InteractionResult.FAIL;
        }

        if (!ProgressionGate.canUseMachine(player, machineId)) {
            player.displayClientMessage(Component.translatable("message.extremecraft.machine_locked", machineId), true);
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer && blockEntity instanceof MenuProvider provider) {
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }

        return InteractionResult.CONSUME;
    }
}

