package com.extremecraft.net;

import com.extremecraft.server.DwServerTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

public record OffhandActionC2S(Action action, int entityId, BlockPos pos, Direction face) {
    public enum Action {ATTACK_ENTITY, USE_ITEM, USE_ON_BLOCK, TAP_BREAK, HOLD_START_BREAK, HOLD_ABORT_BREAK}

    public static void encode(OffhandActionC2S msg, FriendlyByteBuf buf) {
        buf.writeEnum(msg.action);
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.pos != null);
        if (msg.pos != null) {
            buf.writeBlockPos(msg.pos);
            buf.writeEnum(msg.face);
        }
    }

    public static OffhandActionC2S decode(FriendlyByteBuf buf) {
        Action a = buf.readEnum(Action.class);
        int id = buf.readInt();
        BlockPos p = null;
        Direction f = null;
        if (buf.readBoolean()) {
            p = buf.readBlockPos();
            f = buf.readEnum(Direction.class);
        }
        return new OffhandActionC2S(a, id, p, f);
    }

    public static void handle(OffhandActionC2S msg, Supplier<NetworkEvent.Context> ctx) {
        var context = ctx.get();
        context.enqueueWork(() -> {
            ServerPlayer sp = context.getSender();
            if (sp == null) return;
            ServerLevel level = sp.serverLevel();

            switch (msg.action) {
                case ATTACK_ENTITY -> {
                    Entity target = level.getEntity(msg.entityId);
                    if (target != null && withinReach(sp, target.blockPosition())) {
                        invokeOffhandExecutorIfPresent(sp, target);
                    }
                }
                case USE_ITEM -> {
                    ItemStack off = sp.getOffhandItem();
                    if (!off.isEmpty()) {
                        InteractionResultHolder<ItemStack> res = off.use(level, sp, InteractionHand.OFF_HAND);
                        if (res.getResult().consumesAction()) sp.swing(InteractionHand.OFF_HAND, true);
                    }
                }
                case USE_ON_BLOCK -> {
                    if (msg.pos != null && msg.face != null && withinReach(sp, msg.pos)) {
                        BlockHitResult hit = centeredFaceHit(msg.pos, msg.face);
                        UseOnContext useCtx = new UseOnContext(sp, InteractionHand.OFF_HAND, hit);
                        ItemStack off = sp.getOffhandItem();
                        if (!off.isEmpty()) {
                            InteractionResult r = off.useOn(useCtx);
                            if (r.consumesAction()) {
                                sp.swing(InteractionHand.OFF_HAND, true);
                            } else {
                                InteractionResultHolder<ItemStack> fallback = off.use(level, sp, InteractionHand.OFF_HAND);
                                if (fallback.getResult().consumesAction()) sp.swing(InteractionHand.OFF_HAND, true);
                            }
                        }
                    }
                }
                case TAP_BREAK -> {
                    if (msg.pos != null && msg.face != null && withinReach(sp, msg.pos)) {
                        callHandleBlockBreakAction(sp, msg.pos, msg.face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
                        callHandleBlockBreakAction(sp, msg.pos, msg.face, ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK);
                        sp.swing(InteractionHand.OFF_HAND, true);
                    }
                }
                case HOLD_START_BREAK -> {
                    if (msg.pos != null && msg.face != null && withinReach(sp, msg.pos)) {
                        DwServerTicker.startOffhandBreak(sp, msg.pos, msg.face);
                        sp.swing(InteractionHand.OFF_HAND, true);
                    }
                }
                case HOLD_ABORT_BREAK -> DwServerTicker.abortOffhandBreak(sp, true);
            }
        });
        context.setPacketHandled(true);
    }

    private static BlockHitResult centeredFaceHit(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 hit = center.add(face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);
        return new BlockHitResult(hit, face, pos, false);
    }

    public static boolean withinReach(ServerPlayer sp, BlockPos pos) {
        return sp.position().distanceToSqr(Vec3.atCenterOf(pos)) <= 36.0D;
    }

    public static void callHandleBlockBreakAction(ServerPlayer sp, BlockPos pos, Direction face,
                                                  ServerboundPlayerActionPacket.Action action) {
        Objects.requireNonNull(sp);
        try {
            Class<?> gmClass = sp.gameMode.getClass();

            try {
                Method m = gmClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class, int.class, int.class);
                int worldHeight = sp.level().getMaxBuildHeight();
                int seq = 0;
                m.invoke(sp.gameMode, pos, action, face, worldHeight, seq);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method m = gmClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class, int.class);
                int worldHeight = sp.level().getMaxBuildHeight();
                m.invoke(sp.gameMode, pos, action, face, worldHeight);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method m = gmClass.getMethod("handleBlockBreakAction",
                        BlockPos.class, ServerboundPlayerActionPacket.Action.class, Direction.class);
                m.invoke(sp.gameMode, pos, action, face);
                return;
            } catch (NoSuchMethodException ignored) {
            }

            sp.gameMode.destroyBlock(pos);
        } catch (Throwable t) {
            try {
                sp.gameMode.destroyBlock(pos);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void invokeOffhandExecutorIfPresent(ServerPlayer sp, Entity target) {
        try {
            Class<?> clazz = Class.forName("com.extremecraft.net.OffhandExecutor");
            Method m = clazz.getMethod("attackEntityWithOffhand", ServerPlayer.class, Entity.class);
            m.invoke(null, sp, target);
        } catch (Throwable ignored) {
            sp.attack(target);
            sp.swing(InteractionHand.OFF_HAND, true);
        }
    }
}
