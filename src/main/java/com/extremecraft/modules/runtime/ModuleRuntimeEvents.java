package com.extremecraft.modules.runtime;

import com.extremecraft.modules.data.ModuleTrigger;
import com.extremecraft.modules.service.ModuleCatalogSyncService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModuleRuntimeEvents {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModuleRuntimeService.syncState(player);
            ModuleCatalogSyncService.sync(player);
            ModuleRuntimeService.refreshPassiveModifiers(player);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if ((player.tickCount % 20) == 0) {
            ModuleRuntimeService.refreshPassiveModifiers(player);
        }
    }

    @SubscribeEvent
    public void onLivingAttack(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        float reduction = ModuleRuntimeService.shieldReduction(player);
        if (reduction <= 0.0F) {
            return;
        }

        float adjusted = event.getAmount() * (1.0F - reduction);
        event.setAmount(Math.max(0.0F, adjusted));
        ModuleRuntimeService.consumeShieldCooldown(player);
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }

        ItemStack held = player.getMainHandItem();
        if (ModuleRuntimeService.trigger(player, held, ModuleTrigger.ON_RIGHT_CLICK, "main_hand")) {
            event.setCanceled(true);
        }
    }
}
