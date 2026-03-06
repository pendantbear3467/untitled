package com.extremecraft.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

public class HammerItem extends PickaxeItem {
    private static final ThreadLocal<Boolean> HAMMER_ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final float MAX_MINEABLE_HARDNESS = 50.0F;

    public HammerItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        Level level = player.level();
        if (!(level instanceof ServerLevel)) {
            return false;
        }

        // Preserve center-block vanilla mining behavior; only break neighbor blocks here.
        if (HAMMER_ACTIVE.get()) {
            return false;
        }

        Direction face = resolveHitFace(serverPlayer, pos);
        List<BlockPos> aoeBlocks = getAOEBlocks(pos, face);

        HAMMER_ACTIVE.set(Boolean.TRUE);
        try {
            for (int i = 0; i < aoeBlocks.size(); i++) {
                BlockPos target = aoeBlocks.get(i);
                if (target.equals(pos)) {
                    continue;
                }

                if (!canBreakAOETarget(level, target, serverPlayer, stack)) {
                    continue;
                }

                if (level.destroyBlock(target, true, serverPlayer)) {
                    if (stack.isEmpty()) {
                        break;
                    }

                    stack.hurtAndBreak(1, serverPlayer, p -> p.broadcastBreakEvent(resolveBreakSlot(serverPlayer, stack)));
                    if (stack.isEmpty()) {
                        break;
                    }
                }
            }
        } finally {
            HAMMER_ACTIVE.remove();
        }

        // false keeps vanilla behavior for the center block.
        return false;
    }

    private boolean canBreakAOETarget(Level level, BlockPos pos, ServerPlayer player, ItemStack hammerStack) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness > MAX_MINEABLE_HARDNESS) {
            return false;
        }

        if (state.requiresCorrectToolForDrops() && !player.hasCorrectToolForDrops(state)) {
            return false;
        }

        return !state.requiresCorrectToolForDrops() || hammerStack.isCorrectToolForDrops(state);
    }

    private Direction resolveHitFace(ServerPlayer player, BlockPos center) {
        // This is the break-flow equivalent of context.getClickedFace().
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit
            && hit.getType() == HitResult.Type.BLOCK
            && blockHit.getBlockPos().equals(center)) {
            return blockHit.getDirection();
        }

        // Fallback if the server ray-trace doesn't match due to timing/desync.
        return Direction.getNearest(player.getLookAngle().x, player.getLookAngle().y, player.getLookAngle().z).getOpposite();
    }

    public List<BlockPos> getAOEBlocks(BlockPos center, Direction face) {
        List<BlockPos> result = new ArrayList<>(9);

        // Build a 3x3 plane perpendicular to the block face that was hit.
        for (int u = -1; u <= 1; u++) {
            for (int v = -1; v <= 1; v++) {
                BlockPos target = switch (face.getAxis()) {
                    case X -> center.offset(0, u, v); // YZ plane
                    case Y -> center.offset(u, 0, v); // XZ plane
                    case Z -> center.offset(u, v, 0); // XY plane
                };
                result.add(target.immutable());
            }
        }

        return result;
    }

    private EquipmentSlot resolveBreakSlot(Player player, ItemStack stack) {
        Item item = stack.getItem();
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        if (!main.isEmpty() && main.getItem() == item) {
            return EquipmentSlot.MAINHAND;
        }
        if (!off.isEmpty() && off.getItem() == item) {
            return EquipmentSlot.OFFHAND;
        }

        InteractionHand usedHand = player.getUsedItemHand();
        return usedHand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }
}

