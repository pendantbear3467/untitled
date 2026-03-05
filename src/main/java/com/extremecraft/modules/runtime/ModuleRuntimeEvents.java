package com.extremecraft.modules.runtime;

import com.extremecraft.modules.data.ModuleTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModuleRuntimeEvents {
    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModuleRuntimeService.syncState(player);
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

        if ((player.tickCount % 20) != 0) {
            return;
        }

        for (ItemStack armor : player.getArmorSlots()) {
            ModuleRuntimeService.applyPassiveModules(player, armor, null);
        }

        ItemStack held = player.getMainHandItem();
        ModuleRuntimeService.applyPassiveModules(player, held, null);
    }

    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
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
