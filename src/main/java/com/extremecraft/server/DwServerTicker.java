package com.extremecraft.server;

import com.extremecraft.net.OffhandActionC2S;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DwServerTicker {
    private static final ServerboundPlayerActionPacket.Action CONTINUE_BREAK_ACTION =
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK;

    private static final class BreakState {
        final BlockPos pos;
        final Direction face;
        final ItemStack initialItem;

        BreakState(BlockPos pos, Direction face, ItemStack initialItem) {
            this.pos = pos;
            this.face = face;
            this.initialItem = initialItem;
        }
    }

    private static final Map<UUID, BreakState> HOLDING = new HashMap<>();

    public static void startOffhandBreak(ServerPlayer sp, BlockPos pos, Direction face) {
        if (!OffhandActionC2S.withinReach(sp, pos)) return;
        HOLDING.put(sp.getUUID(), new BreakState(pos, face, sp.getOffhandItem().copy()));
        OffhandActionC2S.callHandleBlockBreakAction(sp, pos, face,
                ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
    }

    public static void abortOffhandBreak(ServerPlayer sp, boolean sendStop) {
        BreakState s = HOLDING.remove(sp.getUUID());
        if (s != null && sendStop) {
            OffhandActionC2S.callHandleBlockBreakAction(sp, s.pos, s.face,
                    ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.side != LogicalSide.SERVER || e.phase != TickEvent.Phase.END) return;
        if (!(e.player instanceof ServerPlayer sp)) return;

        BreakState s = HOLDING.get(sp.getUUID());
        if (s == null) return;

        var level = sp.serverLevel();
        if (!level.isLoaded(s.pos)
                || level.isEmptyBlock(s.pos)
                || !OffhandActionC2S.withinReach(sp, s.pos)
                || sp.isSpectator()
                || !ItemStack.matches(sp.getOffhandItem(), s.initialItem)) {
            abortOffhandBreak(sp, true);
            return;
        }

        OffhandActionC2S.callHandleBlockBreakAction(sp, s.pos, s.face, CONTINUE_BREAK_ACTION);

        if (level.isEmptyBlock(s.pos)) {
            abortOffhandBreak(sp, false);
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            abortOffhandBreak(sp, true);
        }
    }
}
