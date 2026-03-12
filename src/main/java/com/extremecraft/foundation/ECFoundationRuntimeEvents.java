package com.extremecraft.foundation;

import com.extremecraft.endgame.EndgameCoreStructureService;
import com.extremecraft.radiation.ContaminationTerrainService;
import com.extremecraft.radiation.RadiationService;
import com.extremecraft.reactor.ReactorMultiblockService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class ECFoundationRuntimeEvents {
    @SubscribeEvent
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        ECDestructiveEffectService.tickLevel(level);
        RadiationService.tickLevel(level);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        RadiationService.tickPlayer(player);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level) {
            ContaminationTerrainService.handleContaminatedBlockBreak(level, event.getPos(), event.getState(), player);
        }

        if (event.getLevel() instanceof Level level) {
            ReactorMultiblockService.invalidateAround(level, event.getPos());
            EndgameCoreStructureService.invalidateAround(level, event.getPos());
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level) {
            ReactorMultiblockService.invalidateAround(level, event.getPos());
            EndgameCoreStructureService.invalidateAround(level, event.getPos());
        }
    }
}
