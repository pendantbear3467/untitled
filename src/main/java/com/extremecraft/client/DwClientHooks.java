package com.extremecraft.client;

import com.extremecraft.config.DwConfig;
import com.extremecraft.net.DwNetwork;
import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.net.OffhandActionC2S.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
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
        if (!DwConfig.CLIENT.enableDualWield.get()) {
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

        // Standard dual-wield behavior: right click on an entity uses offhand attack.
        if (hit instanceof EntityHitResult ehr && isOffhandWeapon(off)) {
            event.setCanceled(true);
            player.swing(InteractionHand.OFF_HAND);
            DwNetwork.sendToServer(new OffhandActionC2S(Action.ATTACK_ENTITY, ehr.getEntity().getId(), null, null));
            return;
        }

        // Advanced override behavior remains opt-in via dedicated keybind.
        if (DwKeybinds.OFFHAND_OVERRIDE == null || !DwKeybinds.OFFHAND_OVERRIDE.isDown()) {
            return;
        }

        event.setCanceled(true);

        if (hit instanceof BlockHitResult bhr) {
            if (player.isShiftKeyDown() && DwConfig.CLIENT.allowOffhandBlockBreaking.get()) {
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
        if (!DwConfig.CLIENT.enableDualWield.get()) {
            return;
        }

        if (DwKeybinds.OFFHAND_OVERRIDE != null && !DwKeybinds.OFFHAND_OVERRIDE.isDown() && isMining) {
            DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_ABORT_BREAK, 0, null, null));
            isMining = false;
        }
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.isCancelable() && event.isCanceled()) {
            DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_ABORT_BREAK, 0, null, null));
        }
    }

    private static boolean isBlacklisted(ItemStack stack) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return getBlacklistedItems().contains(key);
    }

    private static Set<ResourceLocation> getBlacklistedItems() {
        List<? extends String> configured;
        try {
            configured = DwConfig.CLIENT.blacklistedItems.get();
        } catch (Exception e) {
            configured = List.of();
        }

        return configured.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ResourceLocation::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
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
