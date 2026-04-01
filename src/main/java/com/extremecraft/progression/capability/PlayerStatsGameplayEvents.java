package com.extremecraft.progression.capability;

import com.extremecraft.progression.PlayerStatsService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Player-stats runtime hooks that are still live for resource regen, applied break-speed
 * modifiers, and loot modifiers.
 *
 * <p>Gameplay XP and quest/skill progression writes no longer live here. Those mutations are
 * owned by {@code progression.ProgressionEvents} so progression authority is not split across
 * multiple event surfaces.</p>
 */
public class PlayerStatsGameplayEvents {
    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        PlayerStatsApi.get(event.getEntity()).ifPresent(stats -> {
            float bonus = stats.miningSpeedBonus() + stats.blockBreakSpeedBonus();
            if (Math.abs(bonus) < 0.0001F) {
                return;
            }

            float multiplier = Math.max(0.0F, 1.0F + bonus);
            event.setNewSpeed(Math.max(0.0F, event.getOriginalSpeed() * multiplier));
        });
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerStatsApi.get(player).ifPresent(stats -> {
            if (event.getDrops().isEmpty()) {
                return;
            }

            float bonus = stats.lootRarityBonus();
            if (bonus <= 0.0F || player.getRandom().nextFloat() >= Math.min(0.65F, bonus)) {
                return;
            }

            ItemEntity original = event.getDrops().iterator().next();
            ItemStack copy = original.getItem().copy();
            ItemEntity extra = new ItemEntity(event.getEntity().level(), original.getX(), original.getY(), original.getZ(), copy);
            event.getDrops().add(extra);
        });
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



