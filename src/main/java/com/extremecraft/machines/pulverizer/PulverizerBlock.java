package com.extremecraft.machines.pulverizer;

import com.extremecraft.config.Config;
import com.extremecraft.progression.ProgressionGate;
import com.extremecraft.progression.stage.ProgressionStage;
import com.extremecraft.registry.ModBlockEntities;
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

public class PulverizerBlock extends BaseEntityBlock implements EntityBlock {
    public PulverizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.PULVERIZER_BE.get().create(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.PULVERIZER_BE.get(), PulverizerBlockEntity::serverTick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!Config.isMachineEnabled("pulverizer")) {
            player.displayClientMessage(Component.translatable("message.extremecraft.machine_disabled", "pulverizer"), true);
            return InteractionResult.FAIL;
        }

        if (!ProgressionGate.canUseMachine(player, "pulverizer")) {
            String requiredStage = ProgressionGate.requiredMachineStage("pulverizer")
                    .map(ProgressionStage::name)
                    .orElse(ProgressionStage.PRIMITIVE.name());
            player.displayClientMessage(Component.translatable("message.extremecraft.machine_locked", requiredStage), true);
            return InteractionResult.FAIL;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer, provider, pos);
        }
        return InteractionResult.CONSUME;
    }
}

