package com.extremecraft.item.armor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
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

        int interval = 20;
        int offset = Math.floorMod(player.getUUID().hashCode(), interval);
        if (((player.tickCount + offset) % interval) != 0) {
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
            refreshEffect(player, MobEffects.DIG_SPEED, 160, 1);
        }
    }

    private void applyMythrilBonus(ServerPlayer player) {
        if (wearingFullSet(player, "mythril")) {
            refreshEffect(player, MobEffects.REGENERATION, 140, 0);
        }
    }

    private void applyVoidBonus(ServerPlayer player) {
        if (wearingFullSet(player, "void")) {
            refreshEffect(player, MobEffects.DAMAGE_RESISTANCE, 160, 1);
        }
    }

    private void applyAetherBonus(ServerPlayer player) {
        if (wearingFullSet(player, "aether") || wearingFullSet(player, "celestial")) {
            refreshEffect(player, MobEffects.SLOW_FALLING, 160, 0);
            refreshEffect(player, MobEffects.JUMP, 160, 1);
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

    private static void refreshEffect(ServerPlayer player, MobEffect effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > 80) {
            return;
        }

        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }
}
