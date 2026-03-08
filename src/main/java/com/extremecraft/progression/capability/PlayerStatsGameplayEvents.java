package com.extremecraft.progression.capability;

import com.extremecraft.progression.PlayerStatsService;
import com.extremecraft.progression.ProgressionMutationService;
import com.extremecraft.progression.level.LevelService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
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
        int xp = 0;
        if (living.getType() == EntityType.ZOMBIE) {
            xp = 5;
        } else if (living.getType() == EntityType.SKELETON) {
            xp = 6;
        } else if (living.getType() == EntityType.CREEPER) {
            xp = 8;
        }

        if (xp > 0) {
            ProgressionMutationService.grantXp(player, xp);
        }
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
    public void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        int xp = 1;
        if (event.getState().getDestroySpeed(event.getPlayer().level(), event.getPos()) > 5.0F) {
            xp = 2;
        }
        ProgressionMutationService.grantXp(player, xp);
    }

    @SubscribeEvent
    public void onCraft(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(event.getCrafting().getItem()));
        int xp = id.contains("generator") || id.contains("machine") || id.contains("reactor") ? 12 : 3;
        ProgressionMutationService.grantXp(player, xp);
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


