package com.extremecraft.client;

import com.extremecraft.client.gui.player.DualWieldScreen;
import com.extremecraft.client.gui.player.MagicScreen;
import com.extremecraft.client.gui.player.ProgressionScreen;
import com.extremecraft.combat.dualwield.CycleLoadoutC2S;
import com.extremecraft.config.DwConfig;
import com.extremecraft.net.DwNetwork;
import com.extremecraft.net.OffhandActionC2S;
import com.extremecraft.net.OffhandActionC2S.Action;
import com.extremecraft.progression.skilltree.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class DwClientHooks {
    private boolean isMining = false;

    public DwClientHooks() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onInteractKey(InputEvent.InteractionKeyMappingTriggered e) {
        if (DwKeybinds.OFFHAND_OVERRIDE == null || !DwKeybinds.OFFHAND_OVERRIDE.isDown()) return;

        e.setCanceled(true);

        Minecraft mc = Minecraft.getInstance();
        Player pl = mc.player;
        if (pl == null) return;

        HitResult hit = mc.hitResult;
        ItemStack off = pl.getOffhandItem();
        if (off.isEmpty() || isBlacklisted(off) || shouldBypassOverride(off)) return;

        // Crazy Craft-style intent: let offhand drive entity/block/item interaction regardless of item category.
        if (hit instanceof EntityHitResult ehr) {
            pl.swing(InteractionHand.OFF_HAND);
            DwNetwork.sendToServer(new OffhandActionC2S(Action.ATTACK_ENTITY, ehr.getEntity().getId(), null, null));
            return;
        }

        if (hit instanceof BlockHitResult bhr) {
            // Sneak + right mouse = hold-break flow for tools/blocks.
            if (pl.isShiftKeyDown() && DwConfig.CLIENT.allowOffhandBlockBreaking.get()) {
                DwNetwork.sendToServer(new OffhandActionC2S(Action.HOLD_START_BREAK, 0, bhr.getBlockPos(), bhr.getDirection()));
                isMining = true;
                return;
            }

            // Normal interaction on targeted block with OFF_HAND item.
            DwNetwork.sendToServer(new OffhandActionC2S(Action.USE_ON_BLOCK, 0, bhr.getBlockPos(), bhr.getDirection()));
            return;
        }

        DwNetwork.sendToServer(new OffhandActionC2S(Action.USE_ITEM, -1, null, null));
    }

    @SubscribeEvent
    public void onKeyRelease(InputEvent.Key event) {
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

    private static boolean shouldBypassOverride(ItemStack stack) {
        return false;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (DwKeybinds.CYCLE_LOADOUT != null && DwKeybinds.CYCLE_LOADOUT.consumeClick()) {
            DwNetwork.sendToServer(new CycleLoadoutC2S());
        }

        if (DwKeybinds.OPEN_SKILL_TREE != null && DwKeybinds.OPEN_SKILL_TREE.consumeClick()) {
            mc.setScreen(new SkillTreeScreen());
        }

        if (DwKeybinds.OPEN_MAGIC != null && DwKeybinds.OPEN_MAGIC.consumeClick()) {
            mc.setScreen(new MagicScreen(mc.player));
        }

        if (DwKeybinds.OPEN_DUAL_WIELD != null && DwKeybinds.OPEN_DUAL_WIELD.consumeClick()) {
            mc.setScreen(new DualWieldScreen(mc.player));
        }

        if (DwKeybinds.OPEN_PROGRESSION != null && DwKeybinds.OPEN_PROGRESSION.consumeClick()) {
            mc.setScreen(new ProgressionScreen(mc.player));
        }
    }
}
