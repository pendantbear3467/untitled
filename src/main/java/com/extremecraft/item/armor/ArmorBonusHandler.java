package com.extremecraft.item.armor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ArmorBonusHandler {
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        applyTitaniumBonus(player);
        applyMythrilBonus(player);
        applyVoidBonus(player);
        applyAetherBonus(player);
        applyDraconiumFlight(player);
    }

    private void applyTitaniumBonus(ServerPlayer player) {
        if (wearingFullSet(player, "titanium")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 120, 1, true, false, true));
        }
    }

    private void applyMythrilBonus(ServerPlayer player) {
        if (wearingFullSet(player, "mythril")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, true, false, true));
        }
    }

    private void applyVoidBonus(ServerPlayer player) {
        if (wearingFullSet(player, "void")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 1, true, false, true));
        }
    }

    private void applyAetherBonus(ServerPlayer player) {
        if (wearingFullSet(player, "aether") || wearingFullSet(player, "celestial")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, true, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 120, 1, true, false, true));
        }
    }

    private void applyDraconiumFlight(ServerPlayer player) {
        boolean allowFlight = wearingFullSet(player, "draconium") || wearingFullSet(player, "celestial");
        if (allowFlight && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }

        if (!allowFlight && player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private boolean wearingFullSet(ServerPlayer player, String materialId) {
        return armorMatches(player.getItemBySlot(EquipmentSlot.HEAD), materialId + "_helmet")
                && armorMatches(player.getItemBySlot(EquipmentSlot.CHEST), materialId + "_chestplate")
                && armorMatches(player.getItemBySlot(EquipmentSlot.LEGS), materialId + "_leggings")
                && armorMatches(player.getItemBySlot(EquipmentSlot.FEET), materialId + "_boots");
    }

    private boolean armorMatches(ItemStack stack, String pathSuffix) {
        if (stack.isEmpty()) {
            return false;
        }

        return stack.getItem().builtInRegistryHolder().key().location().getPath().equals(pathSuffix);
    }
}
