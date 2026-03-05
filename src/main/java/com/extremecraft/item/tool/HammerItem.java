package com.extremecraft.item.tool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

public class HammerItem extends PickaxeItem {
    public HammerItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return super.onBlockStartBreak(stack, pos, player);
        }

        Level level = serverPlayer.level();
        Direction facing = serverPlayer.getDirection();
        int broken = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos target = switch (facing.getAxis()) {
                    case X -> pos.offset(0, dy, dx);
                    case Y -> pos.offset(dx, 0, dy);
                    case Z -> pos.offset(dx, dy, 0);
                };

                if (target.equals(pos)) {
                    continue;
                }

                if (level.getBlockState(target).getDestroySpeed(level, target) < 0) {
                    continue;
                }

                if (serverPlayer.gameMode.destroyBlock(target)) {
                    broken++;
                }
            }
        }

        if (broken > 0) {
            stack.hurtAndBreak(broken, serverPlayer, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }

        return super.onBlockStartBreak(stack, pos, player);
    }
}
