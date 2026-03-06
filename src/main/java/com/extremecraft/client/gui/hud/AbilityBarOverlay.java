package com.extremecraft.client.gui.hud;

import com.extremecraft.client.input.ExtremeCraftKeybinds;
import com.extremecraft.core.ECConstants;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.network.sync.RuntimeSyncClientState;
import com.extremecraft.ability.AbilityRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Map;

public class AbilityBarOverlay {
    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 6;

    @SubscribeEvent
    public void onRender(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();

        int totalWidth = (SLOT_SIZE * 4) + (SLOT_GAP * 3);
        int startX = (width - totalWidth) / 2;
        int y = height - 44;

        for (int slot = 0; slot < 4; slot++) {
            int x = startX + (slot * (SLOT_SIZE + SLOT_GAP));
            String abilityId = ExtremeCraftKeybinds.resolveAbilityForSlot(slot);
            drawSlot(graphics, minecraft, x, y, slot, abilityId);
        }
    }

    private void drawSlot(GuiGraphics graphics, Minecraft minecraft, int x, int y, int slot, String abilityId) {
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xAA11161F);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xCC1B2432);

        if (abilityId != null && !abilityId.isBlank()) {
            ResourceLocation icon = new ResourceLocation(ECConstants.MODID, "textures/gui/abilities/" + abilityId + ".png");
            graphics.blit(icon, x + 4, y + 4, 0, 0, 16, 16, 16, 16);

            int manaCost = AbilityRegistry.get(abilityId) == null ? 0 : AbilityRegistry.get(abilityId).manaCost();
            graphics.drawString(minecraft.font, Component.literal(String.valueOf(manaCost)), x + 2, y + SLOT_SIZE - 8, 0x8CC8FF, false);

            int cooldown = RuntimeSyncClientState.abilityCooldowns().getOrDefault(abilityId, 0);
            if (cooldown > 0) {
                int assumedMax = Math.max(cooldown, defaultCooldown(abilityId));
                float progress = Math.max(0.0F, Math.min(1.0F, cooldown / (float) assumedMax));
                drawRadialCooldownMask(graphics, x + (SLOT_SIZE / 2), y + (SLOT_SIZE / 2), (SLOT_SIZE / 2) - 2, progress);
                graphics.drawCenteredString(minecraft.font, String.format("%.1f", cooldown / 20.0F), x + (SLOT_SIZE / 2), y + 8, 0xFFE7E7E7);
            }
        }

        graphics.drawCenteredString(minecraft.font, String.valueOf(slot + 1), x + (SLOT_SIZE / 2), y - 8, 0xA8B7CC);
    }

    private void drawRadialCooldownMask(GuiGraphics graphics, int centerX, int centerY, int radius, float progress) {
        int steps = 36;
        int activeSteps = Math.max(0, Math.min(steps, Math.round(steps * progress)));
        for (int i = 0; i < activeSteps; i++) {
            double angle = -Math.PI / 2.0D + ((2.0D * Math.PI * i) / steps);
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            graphics.fill(centerX, centerY, x, y, 0x9910151D);
        }
    }

    private int defaultCooldown(String abilityId) {
        return Map.of(
                "firebolt", 60,
                "blink", 120,
                "arcane_shield", 240,
                "meteor", 320
        ).getOrDefault(abilityId, 100);
    }
}
