package com.extremecraft.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

public class DwServerTicker {
    private static final ServerboundPlayerActionPacket.Action RESUME_DESTROY_BLOCK =
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK;

    private static final class BreakState {
        final BlockPos pos;
        final Direction face;
        final ItemStack initialItem;
        final long startedAtTick;

        BreakState(BlockPos pos, Direction face, ItemStack initialItem, long startedAtTick) {
            this.pos = pos;
            this.face = face;
            this.initialItem = initialItem;
            this.startedAtTick = startedAtTick;
        }

        boolean matches(BlockPos otherPos, Direction otherFace, ItemStack currentItem) {
            return pos.equals(otherPos)
                    && face == otherFace
                    && isTrackedItemEquivalent(currentItem, initialItem);
        }
    }

    public record ActiveBreak(BlockPos pos, Direction face, ItemStack initialItem, long startedAtTick) {
    }

    private static final Map<UUID, BreakState> ACTIVE_BREAKS = new ConcurrentHashMap<>();

    public static boolean canStartOffhandBreak(ServerPlayer player, BlockPos pos, Direction face) {
        if (player == null || pos == null || face == null) {
            return false;
        }

        BreakState active = ACTIVE_BREAKS.get(player.getUUID());
        return active == null || !active.matches(pos, face, player.getOffhandItem());
    }

    public static ActiveBreak getActiveBreak(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        BreakState state = ACTIVE_BREAKS.get(player.getUUID());
        if (state == null) {
            return null;
        }

        return new ActiveBreak(state.pos, state.face, state.initialItem.copy(), state.startedAtTick);
    }

    public static void startOffhandBreak(ServerPlayer sp, BlockPos pos, Direction face) {
        if (!isBreakActionValid(sp, pos, face)) {
            return;
        }

        abortOffhandBreak(sp, true);
        BreakState state = new BreakState(pos.immutable(), face, sp.getOffhandItem().copy(), sp.serverLevel().getGameTime());
        ACTIVE_BREAKS.put(sp.getUUID(), state);
        sendBreakAction(sp, state.pos, state.face, ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
        if (sp.serverLevel().isEmptyBlock(state.pos)) {
            ACTIVE_BREAKS.remove(sp.getUUID());
        }
    }

    public static void abortOffhandBreak(ServerPlayer sp, boolean sendStop) {
        if (sp == null) {
            return;
        }

        BreakState state = ACTIVE_BREAKS.remove(sp.getUUID());
        if (state != null && sendStop) {
            sendBreakAction(sp, state.pos, state.face, ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
        }
    }

    public static void clearPlayer(ServerPlayer player, boolean sendAbort) {
        abortOffhandBreak(player, sendAbort);
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            ACTIVE_BREAKS.remove(playerId);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.side != LogicalSide.SERVER || e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;

        BreakState state = ACTIVE_BREAKS.get(sp.getUUID());
        if (state == null) return;

        if (!isBreakStateStillValid(sp, state)) {
            abortOffhandBreak(sp, true);
            return;
        }

        sendBreakAction(sp, state.pos, state.face, RESUME_DESTROY_BLOCK);

        if (sp.serverLevel().isEmptyBlock(state.pos)) {
            abortOffhandBreak(sp, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, false);
        }
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getSlot() == EquipmentSlot.OFFHAND) {
            abortOffhandBreak(sp, true);
        }
    }

    @SubscribeEvent
    public void onGameModeChange(PlayerEvent.PlayerChangeGameModeEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp && event.getNewGameMode() == GameType.SPECTATOR) {
            abortOffhandBreak(sp, true);
        }
    }

    private static boolean isBreakActionValid(ServerPlayer player, BlockPos pos, Direction face) {
        if (player == null || pos == null || face == null || player.isSpectator() || !player.isAlive()) {
            return false;
        }

        return player.serverLevel().isLoaded(pos)
                && !player.serverLevel().isEmptyBlock(pos)
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= 36.0D
                && !player.getOffhandItem().isEmpty();
    }

    private static boolean isBreakStateStillValid(ServerPlayer player, BreakState state) {
        return player.isAlive()
                && !player.isSpectator()
                && player.connection != null
                && player.serverLevel().isLoaded(state.pos)
                && !player.serverLevel().isEmptyBlock(state.pos)
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(state.pos)) <= 36.0D
                && isTrackedItemEquivalent(player.getOffhandItem(), state.initialItem);
    }

    private static boolean isTrackedItemEquivalent(ItemStack current, ItemStack expected) {
        return ItemStack.isSameItemSameTags(current, expected)
                && current.getCount() == expected.getCount()
                && current.getDamageValue() == expected.getDamageValue();
    }

    private static void sendBreakAction(ServerPlayer player, BlockPos pos, Direction face, ServerboundPlayerActionPacket.Action action) {
        player.gameMode.handleBlockBreakAction(pos, action, face, player.level().getMaxBuildHeight(), 0);
    }
}

