package com.extremecraft.net;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public final class OffhandExecutor {

    /** Reuse vanilla attack logic by temporarily swapping hands. */
    public static void attackEntityWithOffhand(ServerPlayer sp, Entity target) {
        ItemStack main = sp.getMainHandItem().copy();
        ItemStack off  = sp.getOffhandItem().copy();

        try {
            sp.setItemInHand(InteractionHand.MAIN_HAND, off);
            sp.setItemInHand(InteractionHand.OFF_HAND, main);

            sp.swing(InteractionHand.OFF_HAND, true);
            sp.attack(target);
            sp.resetAttackStrengthTicker();
        } finally {
            // Always swap back even if something throws
            sp.setItemInHand(InteractionHand.MAIN_HAND, main);
            sp.setItemInHand(InteractionHand.OFF_HAND, off);
        }
    }
}
