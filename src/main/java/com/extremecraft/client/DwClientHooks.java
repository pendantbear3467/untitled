package com.extremecraft.client;

import com.extremecraft.client.ExtremeCraftKeybinds;
import com.extremecraft.client.gui.debug.DeveloperOverlayState;
import com.extremecraft.config.DwConfig;
import com.extremecraft.net.DwNetwork;
import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.net.OffhandActionC2S.Action;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.ActivateAbilityC2SPacket;
import com.extremecraft.network.packet.ActivateClassAbilityC2SPacket;
import com.extremecraft.network.packet.SpellCastPacket;
import com.extremecraft.progression.classsystem.ability.ClassAbilityClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DwClientHooks {
    private boolean isMining = false;

    @SubscribeEvent
    public void onInteractKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!safeConfigFlag(DwConfig.CLIENT.enableDualWield, true)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.screen != null) {
            return;
        }

        HitResult hit = mc.hitResult;
        ItemStack off = player.getOffhandItem();
        if (off.isEmpty() || isBlacklisted(off) || shouldBypassOverride(off)) {
            return;
        }

        if (hit instanceof EntityHitResult ehr && isOffhandWeapon(off)) {
            event.setCanceled(true);
            player.swing(InteractionHand.OFF_HAND);
            DwNetwork.sendToServer(new OffhandActionC2S(Action.ATTACK_ENTITY, ehr.getEntity().getId(), null, null));
            return;
        }

        if (DwKeybinds.OFFHAND_OVERRIDE == null || !DwKeybinds.OFFHAND_OVERRIDE.isDown()) {
            return;
        }

        event.setCanceled(true);

        if (hit instanceof BlockHitResult bhr) {
            if (player.isShiftKeyDown() && safeConfigFlag(DwConfig.CLIENT.allowOffhandBlockBreaking, true)) {
                DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_START_BREAK, 0, bhr.getBlockPos(), bhr.getDirection()));
                isMining = true;
                return;
            }

            DwNetwork.sendToServer(new OffhandActionC2S(Action.USE_ON_BLOCK, 0, bhr.getBlockPos(), bhr.getDirection()));
            return;
        }

        DwNetwork.sendToServer(new OffhandActionC2S(Action.USE_ITEM, -1, null, null));
    }

    @SubscribeEvent
    public void onKeyRelease(InputEvent.Key event) {
        if (!safeConfigFlag(DwConfig.CLIENT.enableDualWield, true)) {
            return;
        }

        if (DwKeybinds.OFFHAND_OVERRIDE != null && !DwKeybinds.OFFHAND_OVERRIDE.isDown() && isMining) {
            DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_ABORT_BREAK, 0, null, null));
            isMining = false;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClassAbilityClientState.tickDown();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }

        boolean abilityCastTriggered = false;
        abilityCastTriggered |= consumeAbilitySlot(mc.player, ExtremeCraftKeybinds.ABILITY_SLOT_1, 0);
        abilityCastTriggered |= consumeAbilitySlot(mc.player, ExtremeCraftKeybinds.ABILITY_SLOT_2, 1);
        abilityCastTriggered |= consumeAbilitySlot(mc.player, ExtremeCraftKeybinds.ABILITY_SLOT_3, 2);
        abilityCastTriggered |= consumeAbilitySlot(mc.player, ExtremeCraftKeybinds.ABILITY_SLOT_4, 3);

        if (!abilityCastTriggered && DwKeybinds.CLASS_ABILITY != null && DwKeybinds.CLASS_ABILITY.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new ActivateClassAbilityC2SPacket(""));
        }

        if (DwKeybinds.CAST_SPELL != null && DwKeybinds.CAST_SPELL.consumeClick()) {
            ModNetwork.CHANNEL.sendToServer(new SpellCastPacket());
        }

        if (ExtremeCraftKeybinds.DEV_DEBUG_OVERLAY != null && ExtremeCraftKeybinds.DEV_DEBUG_OVERLAY.consumeClick()) {
            boolean enabled = DeveloperOverlayState.toggle();
            mc.player.displayClientMessage(
                    Component.literal("ExtremeCraft developer overlay: " + (enabled ? "ON" : "OFF")),
                    true
            );
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.isCancelable() && event.isCanceled()) {
            DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_ABORT_BREAK, 0, null, null));
        }
    }

    private boolean consumeAbilitySlot(LocalPlayer player, net.minecraft.client.KeyMapping key, int slotIndex) {
        if (key == null || !key.consumeClick()) {
            return false;
        }

        String abilityId = ExtremeCraftKeybinds.resolveAbilityForSlot(player, slotIndex);
        if (abilityId.isBlank()) {
            return true;
        }

        Vec3 target = resolveTargetPosition(player);
        ModNetwork.CHANNEL.sendToServer(new ActivateAbilityC2SPacket(player.getUUID(), abilityId, target));
        return true;
    }

    private Vec3 resolveTargetPosition(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.hitResult != null && minecraft.hitResult.getType() != HitResult.Type.MISS) {
            return minecraft.hitResult.getLocation();
        }

        Vec3 eye = player.getEyePosition();
        return eye.add(player.getLookAngle().scale(16.0D));
    }

    private static boolean isBlacklisted(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return getBlacklistedItems().contains(key);
    }

    private static Set<ResourceLocation> getBlacklistedItems() {
        List<? extends String> configured;
        try {
            configured = DwConfig.CLIENT.blacklistedItems.get();
        } catch (IllegalStateException e) {
            configured = List.of();
        }

        return configured.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean safeConfigFlag(ForgeConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    private static boolean isOffhandWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof DiggerItem;
    }

    private static boolean shouldBypassOverride(ItemStack stack) {
        return false;
    }
}


