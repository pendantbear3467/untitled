package com.extremecraft.entity.system;

import com.extremecraft.config.Config;
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

        if (!Config.COMMON.mobs.enableMachineHazard.get()) {
            return;
        }

        int interval = Math.max(20, Config.COMMON.mobs.machineHazardScanIntervalTicks.get());
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) != 0) {
            return;
        }

        int horizontalRadius = Math.max(1, Config.COMMON.mobs.machineHazardHorizontalRadius.get());
        int verticalRadius = Math.max(1, Config.COMMON.mobs.machineHazardVerticalRadius.get());

        int activeMachines = 0;
        BlockPos center = player.blockPosition();
        ServerLevel level = player.serverLevel();

        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-horizontalRadius, -verticalRadius, -horizontalRadius),
                center.offset(horizontalRadius, verticalRadius, horizontalRadius))) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TechMachineBlockEntity machine
                    && machine.getEnergyStorageExt().getEnergyStored() > machine.getEnergyStorageExt().getMaxEnergyStored() * 0.8F) {
                activeMachines++;
            }
        }

        int requiredMachines = Math.max(1, Config.COMMON.mobs.machineHazardRequiredMachines.get());
        if (activeMachines >= requiredMachines) {
            if (!hasProtection(player)) {
                float damage = (float) Math.max(0.0D, Config.COMMON.mobs.machineHazardDamage.get());
                if (damage > 0.0F) {
                    player.hurt(player.damageSources().magic(), damage);
                }
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
