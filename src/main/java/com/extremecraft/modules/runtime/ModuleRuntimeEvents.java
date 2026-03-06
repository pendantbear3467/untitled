package com.extremecraft.modules.runtime;

import com.extremecraft.combat.event.DamagePreCalculateEvent;
import com.extremecraft.modules.data.ModuleTrigger;
import com.extremecraft.modules.service.ModuleCatalogSyncService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
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
    public void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) {
            ModuleCatalogSyncService.sync(event.getPlayer());
            ModuleRuntimeService.refreshPassiveModifiers(event.getPlayer());
            ModuleRuntimeService.syncState(event.getPlayer());
            return;
        }

        for (ServerPlayer player : event.getPlayerList().getPlayers()) {
            ModuleCatalogSyncService.sync(player);
            ModuleRuntimeService.refreshPassiveModifiers(player);
            ModuleRuntimeService.syncState(player);
        }
    }

    @SubscribeEvent
    public void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
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

        if ((player.tickCount % 200) == 0) {
            ModuleRuntimeService.refreshPassiveModifiers(player);
        }
    }

    @SubscribeEvent
    public void onDamagePreCalculate(DamagePreCalculateEvent event) {
        if (!(event.context().target() instanceof ServerPlayer player)) {
            return;
        }

        float reduction = ModuleRuntimeService.shieldReduction(player);
        if (reduction <= 0.0F) {
            return;
        }

        event.context().modifiers().multiplyStatus(1.0F - reduction);
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