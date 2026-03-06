package com.extremecraft.combat.dualwield.service;

import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.server.DwServerTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executes validated offhand actions on the server thread.
 */
public final class OffhandActionExecutor {
    private static final Logger LOGGER = Logger.getLogger("ExtremeCraft");

    private OffhandActionExecutor() {
    }

    public static void execute(ServerPlayer player, ServerLevel level, OffhandActionC2S packet) {
        if (player == null || level == null || player.level().isClientSide()) {
            return;
        }

        try {
            switch (packet.action()) {
                case ATTACK_ENTITY -> attackEntity(player, level, packet.entityId());
                case USE_ITEM -> useItem(player, level);
                case USE_ON_BLOCK -> useOnBlock(player, level, packet.pos(), packet.face());
                case TAP_BREAK -> tapBreak(player, packet.pos(), packet.face());
                case HOLD_START_BREAK -> holdStartBreak(player, packet.pos(), packet.face());
                case HOLD_ABORT_BREAK -> DwServerTicker.abortOffhandBreak(player, true);
            }
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "Failed to execute offhand action " + packet.action(), ex);
        }
    }

    public static void attackEntity(ServerPlayer player, ServerLevel level, int entityId) {
        Entity target = level.getEntity(entityId);
        if (target == null || !target.isAlive() || target == player) {
            return;
        }

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, off);
            player.setItemInHand(InteractionHand.OFF_HAND, main);

            player.swing(InteractionHand.OFF_HAND, true);
            player.attack(target);
            player.resetAttackStrengthTicker();
        } finally {
            // Swap back using current stacks so durability/breakage changes are preserved.
            ItemStack currentMain = player.getMainHandItem();
            ItemStack currentOff = player.getOffhandItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, currentOff);
            player.setItemInHand(InteractionHand.OFF_HAND, currentMain);
        }
    }

    private static void useItem(ServerPlayer player, ServerLevel level) {
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        InteractionResultHolder<ItemStack> result = off.use(level, player, InteractionHand.OFF_HAND);
        if (result.getResult().consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    private static void useOnBlock(ServerPlayer player, ServerLevel level, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            return;
        }

        BlockHitResult hit = centeredFaceHit(pos, face);
        InteractionResult result = player.gameMode.useItemOn(player, level, off, InteractionHand.OFF_HAND, hit);
        if (result.consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
            return;
        }

        InteractionResultHolder<ItemStack> fallback = off.use(level, player, InteractionHand.OFF_HAND);
        if (fallback.getResult().consumesAction()) {
            player.swing(InteractionHand.OFF_HAND, true);
        }
    }

    private static void tapBreak(ServerPlayer player, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        callHandleBlockBreakAction(player, pos, face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        callHandleBlockBreakAction(player, pos, face, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
        player.swing(InteractionHand.OFF_HAND, true);
    }

    private static void holdStartBreak(ServerPlayer player, BlockPos pos, Direction face) {
        if (pos == null || face == null) {
            return;
        }

        DwServerTicker.startOffhandBreak(player, pos, face);
        player.swing(InteractionHand.OFF_HAND, true);
    }

    public static void callHandleBlockBreakAction(ServerPlayer player, BlockPos pos, Direction face,
                                                  ServerboundPlayerActionPacket.Action action) {
        Objects.requireNonNull(player);
        try {
            Class<?> gameModeClass = player.gameMode.getClass();

            try {
                Method method = gameModeClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class, int.class, int.class);
                int worldHeight = player.level().getMaxBuildHeight();
                int sequence = 0;
                method.invoke(player.gameMode, pos, action, face, worldHeight, sequence);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method method = gameModeClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class, int.class);
                int worldHeight = player.level().getMaxBuildHeight();
                method.invoke(player.gameMode, pos, action, face, worldHeight);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method method = gameModeClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class);
                method.invoke(player.gameMode, pos, action, face);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            LOGGER.log(Level.FINE, "No compatible handleBlockBreakAction signature found for offhand break action");
        } catch (Throwable ex) {
            LOGGER.log(Level.FINE, "Failed to invoke handleBlockBreakAction for offhand action", ex);
        }
    }

    private static BlockHitResult centeredFaceHit(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 hit = center.add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        return new BlockHitResult(hit, face, pos, false);
    }
}

