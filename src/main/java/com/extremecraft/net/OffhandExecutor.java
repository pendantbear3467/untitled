package com.extremecraft.net;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class OffhandExecutor {

    /** Reuse vanilla attack logic by temporarily swapping hands. */
    public static void attackEntityWithOffhand(ServerPlayer sp, Entity target) {
        if (target == null || !target.isAlive() || target == sp) {
            return;
        }

        ItemStack main = sp.getMainHandItem();
        ItemStack off = sp.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        try {
            sp.setItemInHand(InteractionHand.MAIN_HAND, off);
            sp.setItemInHand(InteractionHand.OFF_HAND, main);

            sp.swing(InteractionHand.OFF_HAND, true);
            sp.attack(target);
            sp.resetAttackStrengthTicker();
        } finally {
            // Swap back using current stacks so durability/breakage changes are preserved.
            ItemStack currentMain = sp.getMainHandItem();
            ItemStack currentOff = sp.getOffhandItem();
            sp.setItemInHand(InteractionHand.MAIN_HAND, currentOff);
            sp.setItemInHand(InteractionHand.OFF_HAND, currentMain);
        }
    }
}
