package com.extremecraft.client.gui.player;

import com.extremecraft.core.ECConstants;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.network.sync.RuntimeSyncClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class AbilityBarOverlay {
    private final java.util.Map<String, Integer> cooldownMaxByAbility = new java.util.LinkedHashMap<>();
    private static final ResourceLocation SLOT_BG = new ResourceLocation(ECConstants.MODID, "textures/gui/ability_slot.png");

    private static final int SLOT_SIZE = 22;
    private static final int ICON_SIZE = 16;
    private static final int SLOT_COUNT = 4;

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int totalWidth = (SLOT_COUNT * SLOT_SIZE) + ((SLOT_COUNT - 1) * 4);
        int startX = (screenWidth - totalWidth) / 2;
        int y = screenHeight - 42;

        for (int slotIndex = 0; slotIndex < SLOT_COUNT; slotIndex++) {
            int x = startX + slotIndex * (SLOT_SIZE + 4);
            renderSlot(gui, player, slotIndex, x, y);
        }
    }

    private void renderSlot(GuiGraphics gui, LocalPlayer player, int slotIndex, int x, int y) {
        String abilityId = RuntimeSyncClientState.abilityInSlot(slotIndex);
        if (abilityId == null || abilityId.isBlank()) {
            abilityId = switch (slotIndex) {
                case 0 -> "firebolt";
                case 1 -> "blink";
                case 2 -> "arcane_shield";
                case 3 -> "meteor";
                default -> "";
            };
        }

        int manaCost = RuntimeSyncClientState.manaCostForSlot(slotIndex);
        int cooldownTicks = RuntimeSyncClientState.abilityCooldowns().getOrDefault(abilityId, 0);
        if (cooldownTicks > 0) {
            cooldownMaxByAbility.put(abilityId, Math.max(cooldownTicks, cooldownMaxByAbility.getOrDefault(abilityId, cooldownTicks)));
        } else {
            cooldownMaxByAbility.remove(abilityId);
        }

        gui.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xAA0F1218);
        gui.blit(SLOT_BG, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        if (!abilityId.isBlank()) {
            ResourceLocation icon = new ResourceLocation(ECConstants.MODID, "textures/gui/abilities/" + abilityId + ".png");
            gui.blit(icon, x + 3, y + 3, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }

        if (cooldownTicks > 0) {
            int maxCooldown = Math.max(cooldownMaxByAbility.getOrDefault(abilityId, cooldownTicks), 1);
            float ratio = Math.max(0.0F, Math.min(1.0F, cooldownTicks / (float) maxCooldown));
            drawCooldownRadial(gui, x + 3, y + 3, ICON_SIZE, ratio);
            String label = String.valueOf((int) Math.ceil(cooldownTicks / 20.0D));
            gui.drawCenteredString(Minecraft.getInstance().font, label, x + (SLOT_SIZE / 2), y + 7, 0xFFFFFFFF);
        }

        int currentMana = ManaApi.get(player).map(m -> (int) Math.floor(m.currentMana())).orElse(0);
        int manaColor = currentMana >= manaCost ? 0x88A8E7FF : 0x88FF7A7A;
        gui.fill(x + 2, y + SLOT_SIZE - 4, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x6611141A);
        gui.drawString(Minecraft.getInstance().font, Component.literal(String.valueOf(manaCost)), x + 4, y + SLOT_SIZE - 11, manaColor, false);
    }

    private void drawCooldownRadial(GuiGraphics gui, int x, int y, int size, float ratio) {
        int radius = size / 2;
        int centerX = x + radius;
        int centerY = y + radius;
        double cutoffAngle = ratio * (Math.PI * 2.0D);

        for (int px = 0; px < size; px++) {
            for (int py = 0; py < size; py++) {
                int dx = px - radius;
                int dy = py - radius;
                if ((dx * dx) + (dy * dy) > (radius * radius)) {
                    continue;
                }

                double angle = Math.atan2(dy, dx) + Math.PI;
                if (angle <= cutoffAngle) {
                    gui.fill(centerX + dx, centerY + dy, centerX + dx + 1, centerY + dy + 1, 0xAA000000);
                }
            }
        }
    }
}

