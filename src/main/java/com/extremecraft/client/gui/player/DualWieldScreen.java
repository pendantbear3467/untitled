package com.extremecraft.client.gui.player;

import com.extremecraft.client.DwKeybinds;
import com.extremecraft.combat.dualwield.DualWieldLoadout;
import com.extremecraft.combat.dualwield.PlayerDualWieldApi;
import com.extremecraft.combat.dualwield.PlayerDualWieldData;
import com.extremecraft.combat.dualwield.SaveLoadoutC2S;
import com.extremecraft.combat.dualwield.SelectLoadoutC2S;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.progression.unlock.UnlockAccessService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Dedicated first-release loadout screen for dual-wield management.
 */
public class DualWieldScreen extends StandalonePlayerScreen {
    private final Screen returnScreen;

    public DualWieldScreen(Player player) {
        this(player, null);
    }

    public DualWieldScreen(Player player, Screen returnScreen) {
        super(player, Component.literal("Dual Wield Loadouts"));
        this.returnScreen = returnScreen;
        this.panelWidth = 320;
        this.panelHeight = 224;
    }

    @Override
    protected void init() {
        super.init();

        int rowY = panelTop + 86;
        int applyX = panelLeft + 210;
        int saveX = panelLeft + 258;
        for (int i = 0; i < 3; i++) {
            final int loadoutIndex = i;
            addRenderableWidget(Button.builder(Component.literal("Use"), b ->
                            ModNetwork.CHANNEL.sendToServer(new SelectLoadoutC2S(loadoutIndex)))
                    .bounds(applyX, rowY + (i * 42), 42, 18)
                    .build());
            addRenderableWidget(Button.builder(Component.literal("Save"), b ->
                            ModNetwork.CHANNEL.sendToServer(new SaveLoadoutC2S(loadoutIndex)))
                    .bounds(saveX, rowY + (i * 42), 46, 18)
                    .build());
        }

        if (returnScreen != null) {
            addRenderableWidget(Button.builder(Component.literal("Back"), b -> Minecraft.getInstance().setScreen(returnScreen))
                    .bounds(panelLeft + 210, panelTop + panelHeight - 30, 94, 18)
                    .build());
        }
    }

    @Override
    protected void drawStandaloneContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Optional<PlayerDualWieldData> dualWieldOpt = PlayerDualWieldApi.get(player);
        int activeLoadout = dualWieldOpt.map(data -> data.activeLoadoutIndex() + 1).orElse(1);

        int headerX = panelLeft + 18;
        int headerY = panelTop + 44;
        guiGraphics.drawString(this.font, Component.literal("Separate loadout management for combat, mining, ranged, and hybrid play."), headerX, headerY, 0xC9D5E6, false);
        guiGraphics.drawString(this.font, Component.literal("Active loadout: L" + activeLoadout + "    Cycle: " + keyLabel(DwKeybinds.CYCLE_LOADOUT)), headerX, headerY + 12, 0xE8C78D, false);
        guiGraphics.drawString(this.font, Component.literal("Offhand override: " + keyLabel(DwKeybinds.OFFHAND_OVERRIDE) + "    Spell: " + keyLabel(DwKeybinds.CAST_SPELL)), headerX, headerY + 24, 0x9FB7D7, false);

        drawCurrentHands(guiGraphics, panelLeft + 18, panelTop + 68);

        int rowY = panelTop + 86;
        for (int i = 0; i < 3; i++) {
            final int loadoutIndex = i;
            int cardTop = rowY + (i * 42);
            boolean selected = (i + 1) == activeLoadout;
            DualWieldLoadout loadout = dualWieldOpt.map(data -> data.getLoadout(loadoutIndex)).orElse(null);
            drawLoadoutCard(guiGraphics, mouseX, mouseY, loadoutIndex, cardTop, selected, loadout);
        }

        guiGraphics.drawString(this.font, Component.literal("Alpha: direct save/apply, role labels, active-state clarity, and inventory access."), headerX, panelTop + panelHeight - 34, 0xAAB8CA, false);
        guiGraphics.drawString(this.font, Component.literal("Beta: drag-drop assignment, filtered slot acceptance, and tighter inventory adjacency."), headerX, panelTop + panelHeight - 22, 0xAAB8CA, false);
    }

    private void drawCurrentHands(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.drawString(this.font, Component.literal("Live Hands"), x, y, 0xDDE5F1, false);
        drawItemSlot(guiGraphics, x, y + 12, player.getMainHandItem());
        drawItemSlot(guiGraphics, x + 22, y + 12, player.getOffhandItem());
        guiGraphics.drawString(this.font, Component.literal("Main"), x, y + 34, 0x9FB7D7, false);
        guiGraphics.drawString(this.font, Component.literal("Off"), x + 22, y + 34, 0x9FB7D7, false);
    }

    private void drawLoadoutCard(GuiGraphics guiGraphics,
                                 int mouseX,
                                 int mouseY,
                                 int loadoutIndex,
                                 int top,
                                 boolean selected,
                                 DualWieldLoadout loadout) {
        int left = panelLeft + 18;
        int right = panelLeft + 202;
        int frameColor = selected ? 0xAA5A4430 : 0xAA202633;
        int innerColor = selected ? 0x88331F12 : 0x6611141C;

        guiGraphics.fill(left, top, right, top + 34, frameColor);
        guiGraphics.fill(left + 1, top + 1, right - 1, top + 33, innerColor);
        guiGraphics.drawString(this.font, Component.literal((selected ? "> " : "  ") + "Loadout " + (loadoutIndex + 1)), left + 8, top + 5, selected ? 0xF1DEB9 : 0xD7E0EE, false);

        ItemStack main = loadout == null ? ItemStack.EMPTY : loadout.mainHandItem();
        ItemStack off = loadout == null ? ItemStack.EMPTY : loadout.offHandItem();

        drawItemSlot(guiGraphics, left + 8, top + 14, main);
        drawItemSlot(guiGraphics, left + 30, top + 14, off);
        guiGraphics.drawString(this.font, Component.literal("Main: " + compactItemLabel(main)), left + 56, top + 14, 0xD7E0EE, false);
        guiGraphics.drawString(this.font, Component.literal("Off: " + compactItemLabel(off)), left + 56, top + 24, 0xB7C6D9, false);

        boolean mainUnlocked = UnlockAccessService.canUseLoadoutSlot(player, "main_hand");
        boolean offUnlocked = UnlockAccessService.canUseLoadoutSlot(player, "off_hand");
        guiGraphics.drawString(this.font, Component.literal("Main slot: " + (mainUnlocked ? "Unlocked" : "Locked")), left + 148, top + 5, mainUnlocked ? 0x8FE2A5 : 0xFFAA88, false);
        guiGraphics.drawString(this.font, Component.literal("Off slot: " + (offUnlocked ? "Unlocked" : "Locked")), left + 148, top + 16, offUnlocked ? 0x8FE2A5 : 0xFFAA88, false);

        if (mouseX >= left + 8 && mouseX <= left + 26 && mouseY >= top + 14 && mouseY <= top + 32 && !main.isEmpty()) {
            guiGraphics.renderTooltip(this.font, main, mouseX, mouseY);
        }
        if (mouseX >= left + 30 && mouseX <= left + 48 && mouseY >= top + 14 && mouseY <= top + 32 && !off.isEmpty()) {
            guiGraphics.renderTooltip(this.font, off, mouseX, mouseY);
        }
    }

    private void drawItemSlot(GuiGraphics guiGraphics, int x, int y, ItemStack stack) {
        guiGraphics.fill(x, y, x + 18, y + 18, 0xAA1F2432);
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, 0x660A0C11);
        guiGraphics.renderItem(stack, x + 1, y + 1);
    }

    private static String compactItemLabel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "-";
        }

        String label = stack.getHoverName().getString();
        if (label.length() <= 16) {
            return label;
        }
        return label.substring(0, 13) + "...";
    }

    private static String keyLabel(KeyMapping keyMapping) {
        return keyMapping == null ? "[unbound]" : "[" + keyMapping.getTranslatedKeyMessage().getString() + "]";
    }

    @Override
    public void onClose() {
        if (returnScreen != null) {
            Minecraft.getInstance().setScreen(returnScreen);
            return;
        }
        super.onClose();
    }
}
