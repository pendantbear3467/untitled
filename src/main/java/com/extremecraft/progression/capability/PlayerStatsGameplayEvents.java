package com.extremecraft.progression.capability;

import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class PlayerStatsGameplayEvents {
    @SubscribeEvent
    public void onMobKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        LivingEntity living = event.getEntity();
        int xp = Math.max(4, (int) living.getMaxHealth() / 2);
        PlayerStatsService.addExperience(player, xp);
    }

    @SubscribeEvent
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        int xp = 1;
        if (event.getState().getDestroySpeed(event.getPlayer().level(), event.getPos()) > 5.0F) {
            xp = 2;
        }
        PlayerStatsService.addExperience(player, xp);
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem()));
        int xp = id.contains("generator") || id.contains("machine") || id.contains("reactor") ? 12 : 3;
        PlayerStatsService.addExperience(player, xp);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (event.player instanceof ServerPlayer player) {
            PlayerStatsService.tickResources(player);
        }
    }
}
