package com.extremecraft.client.gui.player;

import com.extremecraft.core.ECConstants;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExtremePlayerScreen extends Screen {
    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(ECConstants.MODID, "textures/gui/extreme_player_menu.png");
    private static final ResourceLocation MAGIC_SLOT = new ResourceLocation(ECConstants.MODID, "textures/gui/magic_slot.png");

    private static final int GUI_WIDTH = 306;
    private static final int GUI_HEIGHT = 206;
    private static final int TAB_WIDTH = 58;
    private static final int TAB_HEIGHT = 18;

    private final Player player;
    private final Screen returnScreen;
    private final SkillTreeScreenPanel skillTreePanel;
    private final List<Button> statButtons = new ArrayList<>();

    private ExtremePlayerTabs.Tab activeTab = ExtremePlayerTabs.Tab.PLAYER_STATS;
    private String activeSkillTree = "combat";

    private int leftPos;
    private int topPos;

    public ExtremePlayerScreen(Player player, Screen returnScreen) {
        super(Component.literal("ExtremeCraft Player"));
        this.player = player;
        this.returnScreen = returnScreen;
        this.skillTreePanel = new SkillTreeScreenPanel(this::getStats);
    }

    public ExtremePlayerScreen(Player player, ExtremePlayerTabs.Tab defaultTab) {
        this(player, Minecraft.getInstance().screen instanceof InventoryScreen inv ? inv : null);
        this.activeTab = defaultTab;
    }

    @Override
    protected void init() {
        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;

        int tabX = leftPos + 8;
        int tabY = topPos + 8;
        for (ExtremePlayerTabs.Tab tab : ExtremePlayerTabs.Tab.values()) {
            final ExtremePlayerTabs.Tab captured = tab;
            addRenderableWidget(Button.builder(tab.label(), b -> {
                        activeTab = captured;
                        updateStatButtonVisibility();
                    }).bounds(tabX, tabY, TAB_WIDTH, TAB_HEIGHT)
                    .build());
            tabX += TAB_WIDTH + 2;
        }

        int panelX = leftPos + 200;
        int rowY = topPos + 74;
        statButtons.add(addRenderableWidget(statButton("vitality", panelX, rowY)));
        statButtons.add(addRenderableWidget(statButton("strength", panelX, rowY + 20)));
        statButtons.add(addRenderableWidget(statButton("agility", panelX, rowY + 40)));
        statButtons.add(addRenderableWidget(statButton("endurance", panelX, rowY + 60)));
        statButtons.add(addRenderableWidget(statButton("intelligence", panelX, rowY + 80)));
        statButtons.add(addRenderableWidget(statButton("luck", panelX, rowY + 100)));

        updateStatButtonVisibility();
        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());
    }

    private Button statButton(String stat, int x, int y) {
        return Button.builder(Component.literal("+"), b -> ModNetwork.CHANNEL.sendToServer(new UpgradeStatPacket(stat)))
                .bounds(x, y, 18, 16)
                .build();
    }

    private void updateStatButtonVisibility() {
        boolean visible = activeTab == ExtremePlayerTabs.Tab.PLAYER_STATS;
        for (Button b : statButtons) {
            b.visible = visible;
            b.active = visible;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        renderFrame(graphics);

        switch (activeTab) {
            case PLAYER_STATS -> renderStatsTab(graphics);
            case SKILLS -> renderSkillsTab(graphics, mouseX, mouseY);
            case MAGIC -> renderMagicTab(graphics, mouseX, mouseY);
            case DUAL_WIELD -> renderDualWieldTab(graphics, mouseX, mouseY);
            case CLASS_SKILLS -> renderClassTab(graphics);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFrame(GuiGraphics graphics) {
        if (minecraft != null && minecraft.getResourceManager().getResource(BG_TEXTURE).isPresent()) {
            graphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        } else {
            graphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xF01A1B22);
            graphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xE0282B35);
        }

        int pulse = 80 + (int) (Math.sin((player.tickCount + minecraft.getFrameTime()) * 0.07F) * 30);
        int glow = (Mth.clamp(pulse, 30, 140) << 24) | 0x00468DFF;
        graphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + 4, glow);

        graphics.drawString(font, Component.literal("ExtremeCraft Progression"), leftPos + 10, topPos + 32, 0xF3D6A0, false);
        graphics.fill(leftPos + 8, topPos + 52, leftPos + GUI_WIDTH - 8, topPos + GUI_HEIGHT - 10, 0xAA0C0F16);
    }

    private void renderStatsTab(GuiGraphics graphics) {
        Optional<PlayerStatsCapability> statsOpt = getStats();
        int x = leftPos + 16;
        int y = topPos + 62;

        if (statsOpt.isEmpty()) {
            graphics.drawString(font, Component.literal("Syncing stats..."), x, y, 0xD5D9E2, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Level: " + stats.level()), x, y, 0xEAEFF7, false);
        graphics.drawString(font, Component.literal("XP: " + stats.experience() + " / " + stats.experienceToNextLevel()), x, y + 12, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Stat Points: " + stats.statPoints()), x, y + 24, 0xF1C98F, false);
        graphics.drawString(font, Component.literal("Skill Points: " + stats.skillPoints()), x, y + 36, 0xCFB5FF, false);

        drawPrimaryRow(graphics, "Vitality", stats.vitality(), x, y + 56);
        drawPrimaryRow(graphics, "Strength", stats.strength(), x, y + 76);
        drawPrimaryRow(graphics, "Agility", stats.agility(), x, y + 96);
        drawPrimaryRow(graphics, "Endurance", stats.endurance(), x, y + 116);
        drawPrimaryRow(graphics, "Intelligence", stats.intelligence(), x, y + 136);
        drawPrimaryRow(graphics, "Luck", stats.luck(), x, y + 156);

        int rx = leftPos + 236;
        graphics.drawString(font, Component.literal("HP " + stats.maxHealth()), rx, y, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Mana " + stats.maxMana()), rx, y + 12, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Crit " + (int) (stats.critChance() * 100) + "%"), rx, y + 24, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("MS " + String.format("%.2f", stats.movementSpeed())), rx, y + 36, 0xE6EAF2, false);
    }

    private void drawPrimaryRow(GuiGraphics graphics, String label, int value, int x, int y) {
        graphics.drawString(font, Component.literal(label), x, y, 0xD9DEEA, false);
        graphics.fill(x + 74, y + 3, x + 168, y + 10, 0xAA212735);
        int fill = Math.min(94, (int) (value / 40.0F * 94));
        graphics.fill(x + 75, y + 4, x + 75 + fill, y + 9, 0xFF4B7FC9);
        graphics.drawString(font, Component.literal(String.valueOf(value)), x + 172, y, 0xF0F4FA, false);
    }

    private void renderSkillsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 14;
        int y = topPos + 58;
        int w = GUI_WIDTH - 28;
        int h = GUI_HEIGHT - 70;

        graphics.drawString(font, Component.literal("Skill Tree"), x, y - 10, 0xE8C78D, false);

        int treeButtonX = x;
        for (String treeId : SkillTreeManager.treeIds()) {
            int color = treeId.equals(activeSkillTree) ? 0xFFD6B37A : 0xFF9EA7B6;
            graphics.drawString(font, Component.literal("[" + treeId + "]"), treeButtonX, y + h - 12, color, false);
            treeButtonX += 62;
        }

        skillTreePanel.render(graphics, font, x, y, w, h - 16, activeSkillTree, mouseX, mouseY);
    }

    private void renderMagicTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        Optional<PlayerStatsCapability> statsOpt = getStats();
        if (statsOpt.isEmpty()) {
            graphics.drawString(font, Component.literal("Syncing magic..."), x, y, 0xD5D9E2, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        graphics.drawString(font, Component.literal("Mana: " + stats.mana() + " / " + stats.maxMana()), x, y, 0xCFAEFF, false);
        graphics.drawString(font, Component.literal("Magic Power: " + stats.magicPower()), x, y + 14, 0xCFAEFF, false);

        int time = player.tickCount;
        for (int i = 0; i < 6; i++) {
            int sx = x + i * 28;
            int sy = y + 36;

            if (minecraft != null && minecraft.getResourceManager().getResource(MAGIC_SLOT).isPresent()) {
                int frame = (time / 6) % 4;
                graphics.blit(MAGIC_SLOT, sx, sy, frame * 22, 0, 22, 22, 88, 22);
            } else {
                int pulse = (80 + Math.abs(10 - ((time + i * 3) % 20)) * 6) << 24;
                graphics.fill(sx, sy, sx + 22, sy + 22, pulse | 0x003E5C96);
                graphics.drawCenteredString(font, Component.literal("R"), sx + 11, sy + 7, 0xFFE8D6FF);
            }

            if (mouseX >= sx && mouseX <= sx + 22 && mouseY >= sy && mouseY <= sy + 22) {
                graphics.renderTooltip(font, Component.literal("Rune Slot " + (i + 1)), mouseX, mouseY);
            }
        }
    }

    private void renderDualWieldTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        graphics.drawString(font, Component.literal("Main Hand"), x, y, 0xD8DEE9, false);
        graphics.drawString(font, Component.literal("Offhand"), x + 100, y, 0xD8DEE9, false);

        drawItemSlot(graphics, x, y + 14, main);
        drawItemSlot(graphics, x + 100, y + 14, off);

        if (mouseX >= x && mouseX <= x + 18 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, main, mouseX, mouseY);
        }
        if (mouseX >= x + 100 && mouseX <= x + 118 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, off, mouseX, mouseY);
        }
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        graphics.fill(x, y, x + 18, y + 18, 0xAA1F2432);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x660A0C11);
        graphics.renderItem(stack, x + 1, y + 1);
    }

    private void renderClassTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 62;

        graphics.drawString(font, Component.literal("Class System (Future Expansion)"), x, y, 0xE3CDA0, false);
        graphics.drawString(font, Component.literal("Reserved for specialization branches and class perks."), x, y + 16, 0xC8CFDB, false);
    }

    private Optional<PlayerStatsCapability> getStats() {
        return PlayerStatsApi.get(player);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == ExtremePlayerTabs.Tab.SKILLS && button == InputConstants.MOUSE_BUTTON_LEFT) {
            int panelX = leftPos + 14;
            int panelY = topPos + 58;
            int panelW = GUI_WIDTH - 28;
            int panelH = GUI_HEIGHT - 70;
            if (skillTreePanel.mouseClicked(panelX, panelY, panelW, panelH - 16, activeSkillTree, mouseX, mouseY)) {
                return true;
            }

            int treeButtonX = panelX;
            int treeButtonY = panelY + panelH - 12;
            for (String treeId : SkillTreeManager.treeIds()) {
                if (mouseX >= treeButtonX && mouseX <= treeButtonX + 56 && mouseY >= treeButtonY && mouseY <= treeButtonY + 10) {
                    activeSkillTree = treeId;
                    return true;
                }
                treeButtonX += 62;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(returnScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
