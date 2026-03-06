package com.extremecraft.entity.system;

import com.extremecraft.machine.core.TechMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GameplayMechanicsEvents {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.tickCount % 100 != 0) {
            return;
        }

        int activeMachines = 0;
        BlockPos center = player.blockPosition();
        ServerLevel level = player.serverLevel();

        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-8, -3, -8), center.offset(8, 3, 8))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TechMachineBlockEntity machine && machine.getEnergyStorageExt().getEnergyStored() > machine.getEnergyStorageExt().getMaxEnergyStored() * 0.8F) {
                activeMachines++;
            }
        }

        if (activeMachines >= 4) {
            boolean protectedByGear = hasProtection(player);
            if (!protectedByGear) {
                player.hurt(player.damageSources().magic(), 2.0F);
            } else {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 0));
            }
        }
    }

    @SubscribeEvent
    public void onCombatCombo(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        boolean combo = player.hasEffect(MobEffects.DAMAGE_BOOST) && player.hasEffect(MobEffects.MOVEMENT_SPEED);
        if (combo) {
            event.setAmount(event.getAmount() * 1.15F);
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60, 0));
        }
    }

    private boolean hasProtection(ServerPlayer player) {
        ItemStack chest = player.getInventory().armor.get(2);
        return !chest.isEmpty() && chest.getDescriptionId().contains("pioneer_chestplate");
    }
}

