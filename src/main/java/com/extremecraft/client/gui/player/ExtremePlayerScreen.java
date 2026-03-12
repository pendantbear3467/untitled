package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.layout.GuiScaleContext;
import com.extremecraft.config.DwConfig;
import com.extremecraft.core.ECConstants;
import com.extremecraft.classsystem.ClassRegistry;
import com.extremecraft.classsystem.PlayerClass;
import com.extremecraft.magic.mana.ManaApi;
import com.extremecraft.magic.Spell;
import com.extremecraft.magic.SpellRegistry;
import com.extremecraft.modules.runtime.ModuleCatalogClientState;
import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.InstallModuleC2SPacket;
import com.extremecraft.network.packet.RemoveModuleC2SPacket;
import com.extremecraft.network.packet.RequestPlayerStatsPacket;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.capability.PlayerStatsApi;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.capability.ProgressApi;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import com.extremecraft.quest.QuestManager;
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
import java.util.Locale;

public class ExtremePlayerScreen extends Screen {
    private static final ResourceLocation BG_TEXTURE = new ResourceLocation(ECConstants.MODID, "textures/gui/extreme_player_menu.png");
    private static final ResourceLocation MAGIC_SLOT = new ResourceLocation(ECConstants.MODID, "textures/gui/magic_slot.png");

    private static final int BASE_TEXTURE_WIDTH = 306;
    private static final int BASE_TEXTURE_HEIGHT = 206;

    private static final int TAB_WIDTH = 58;
    private static final int TAB_HEIGHT = 18;
    private static final int MODULES_PAGE_SIZE = 4;

    private final Player player;
    private final Screen returnScreen;
    private final SkillTreeScreenPanel skillTreePanel;
    private final List<Button> tabButtons = new ArrayList<>();
    private final List<Button> statButtons = new ArrayList<>();

    private ExtremePlayerTabs.Tab activeTab = ExtremePlayerTabs.Tab.PLAYER_STATS;
    private String activeSkillTree = "combat";

    private String moduleSearchQuery = "";
    private int toolPage = 0;
    private int armorPage = 0;
    private int selectedSpellIndex = 0;

    private int guiWidth;
    private int guiHeight;
    private int leftPos;
    private int topPos;
    private GuiScaleContext layoutContext;

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
        tabButtons.clear();
        statButtons.clear();

        for (ExtremePlayerTabs.Tab tab : ExtremePlayerTabs.Tab.values()) {
            final ExtremePlayerTabs.Tab captured = tab;
            Button button = Button.builder(tab.label(), b -> setActiveTab(captured))
                    .bounds(0, 0, TAB_WIDTH, TAB_HEIGHT)
                    .build();
            tabButtons.add(button);
            addRenderableWidget(button);
        }

        statButtons.add(addRenderableWidget(statButton("vitality")));
        statButtons.add(addRenderableWidget(statButton("strength")));
        statButtons.add(addRenderableWidget(statButton("agility")));
        statButtons.add(addRenderableWidget(statButton("endurance")));
        statButtons.add(addRenderableWidget(statButton("intelligence")));
        statButtons.add(addRenderableWidget(statButton("luck")));

        recalculateLayout();
        updateStatButtonVisibility();
        ModNetwork.CHANNEL.sendToServer(new RequestPlayerStatsPacket());
    }

    private void setActiveTab(ExtremePlayerTabs.Tab tab) {
        if (activeTab == tab) {
            return;
        }

        activeTab = tab;
        recalculateLayout();
        updateStatButtonVisibility();
    }

    private Button statButton(String stat) {
        return Button.builder(Component.literal("+"), b -> ModNetwork.CHANNEL.sendToServer(new UpgradeStatPacket(stat)))
                .bounds(0, 0, 18, 16)
                .build();
    }

    private void recalculateLayout() {
        if (minecraft == null) {
            return;
        }

        layoutContext = GuiScaleContext.from(
                width,
                height,
                activeTab.preferredWidth(),
                activeTab.preferredHeight(),
                DwConfig.CLIENT.guiScaleMultiplier.get(),
                0.75D,
                1.75D,
                320,
                214
        );

        guiWidth = layoutContext.contentWidth();
        guiHeight = layoutContext.contentHeight();
        leftPos = layoutContext.left();
        topPos = layoutContext.top();

        int tabsStartX = leftPos + 10;
        int tabsY = topPos + 8;
        for (int i = 0; i < tabButtons.size(); i++) {
            Button button = tabButtons.get(i);
            button.setX(tabsStartX + (i * (TAB_WIDTH + 4)));
            button.setY(tabsY);
        }

        int rowY = topPos + 89;
        int buttonX = leftPos + 214;
        for (int i = 0; i < statButtons.size(); i++) {
            Button button = statButtons.get(i);
            button.setX(buttonX);
            button.setY(rowY + (i * 24));
        }
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
            case MODULES -> renderModulesTab(graphics, mouseX, mouseY);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderFrame(GuiGraphics graphics) {
        if (minecraft != null && minecraft.getResourceManager().getResource(BG_TEXTURE).isPresent()) {
            graphics.blit(BG_TEXTURE, leftPos, topPos, 0, 0, guiWidth, guiHeight, BASE_TEXTURE_WIDTH, BASE_TEXTURE_HEIGHT);
        } else {
            graphics.fill(leftPos, topPos, leftPos + guiWidth, topPos + guiHeight, 0xF01A1B22);
            graphics.fill(leftPos + 2, topPos + 2, leftPos + guiWidth - 2, topPos + guiHeight - 2, 0xE0282B35);
        }

        int pulse = 80 + (int) (Math.sin((player.tickCount + (minecraft == null ? 0.0F : minecraft.getFrameTime())) * 0.07F) * 30);
        int glow = (Mth.clamp(pulse, 30, 140) << 24) | 0x00468DFF;
        graphics.fill(leftPos + 2, topPos + 2, leftPos + guiWidth - 2, topPos + 4, glow);

        graphics.drawString(font, Component.literal("ExtremeCraft Progression"), leftPos + 10, topPos + 34, 0xF3D6A0, false);
        graphics.fill(leftPos + 8, topPos + 54, leftPos + guiWidth - 8, topPos + guiHeight - 10, 0xAA0C0F16);
    }

    private void renderStatsTab(GuiGraphics graphics) {
        Optional<PlayerStatsCapability> statsOpt = getStats();
        Optional<PlayerProgressData> progressOpt = ProgressApi.get(player);
        int x = leftPos + 16;
        int y = topPos + 62;

        if (statsOpt.isEmpty()) {
            graphics.drawString(font, Component.literal("Syncing stats..."), x, y, 0xD5D9E2, false);
            return;
        }

        PlayerStatsCapability stats = statsOpt.get();
        int playerLevel = progressOpt.map(PlayerProgressData::level).orElse(stats.level());
        int playerXp = progressOpt.map(PlayerProgressData::xp).orElse(stats.experience());
        int xpToNext = progressOpt.map(data -> PlayerProgressData.xpToNextLevel(data.level())).orElse(stats.experienceToNextLevel());
        int playerSkillPoints = progressOpt.map(PlayerProgressData::playerSkillPoints).orElse(stats.skillPoints());

        graphics.drawString(font, Component.literal("Level: " + playerLevel), x, y, 0xEAEFF7, false);
        graphics.drawString(font, Component.literal("Stat Points: " + stats.statPoints()), x + 80, y, 0xF1C98F, false);
        graphics.drawString(font, Component.literal("Player Skill Points: " + playerSkillPoints), x + 152, y, 0xCFB5FF, false);

        drawXpBar(graphics, x, y + 14, 220, playerXp, xpToNext, 0xFF4B8AE2, 0xFF6AAEF2);

        int rowStartY = y + 40;
        drawPrimaryRow(graphics, "Vitality", stats.vitality(), x, rowStartY);
        drawPrimaryRow(graphics, "Strength", stats.strength(), x, rowStartY + 24);
        drawPrimaryRow(graphics, "Agility", stats.agility(), x, rowStartY + 48);
        drawPrimaryRow(graphics, "Endurance", stats.endurance(), x, rowStartY + 72);
        drawPrimaryRow(graphics, "Intelligence", stats.intelligence(), x, rowStartY + 96);
        drawPrimaryRow(graphics, "Luck", stats.luck(), x, rowStartY + 120);

        int rightPanelX = leftPos + guiWidth - 108;
        int rightPanelY = y + 40;
        int manaCap = ManaApi.get(player).map(mana -> (int) Math.ceil(mana.maxMana())).orElse(stats.maxMana());
        graphics.drawString(font, Component.literal("HP " + stats.maxHealth()), rightPanelX, rightPanelY, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Mana Cap " + manaCap), rightPanelX, rightPanelY + 14, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Crit " + (int) (stats.critChance() * 100) + "%"), rightPanelX, rightPanelY + 28, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Move " + String.format("%.2f", stats.movementSpeed())), rightPanelX, rightPanelY + 42, 0xE6EAF2, false);
        graphics.drawString(font, Component.literal("Magic " + stats.magicPower()), rightPanelX, rightPanelY + 56, 0xE6EAF2, false);
    }

    private void drawPrimaryRow(GuiGraphics graphics, String label, int value, int x, int y) {
        graphics.drawString(font, Component.literal(label), x, y + 3, 0xD9DEEA, false);

        int barLeft = x + 72;
        int barWidth = 104;
        graphics.fill(barLeft, y + 4, barLeft + barWidth, y + 11, 0xAA212735);
        int fill = Math.min(barWidth - 1, (int) ((Math.min(40, value) / 40.0F) * (barWidth - 1)));
        graphics.fill(barLeft + 1, y + 5, barLeft + 1 + fill, y + 10, 0xFF4B7FC9);

        graphics.drawString(font, Component.literal(String.valueOf(value)), x + 182, y + 3, 0xF0F4FA, false);
    }

    private void drawXpBar(GuiGraphics graphics, int x, int y, int width, int xp, int xpNeeded, int leftColor, int rightColor) {
        int clampedNeeded = Math.max(1, xpNeeded);
        int fill = Math.min(width, Math.round((xp / (float) clampedNeeded) * width));

        graphics.fill(x, y, x + width, y + 8, 0xAA1A1F2A);
        if (fill > 0) {
            graphics.fillGradient(x, y, x + fill, y + 8, leftColor, rightColor);
        }

        Component xpText = Component.literal(xp + " / " + clampedNeeded + " XP");
        graphics.drawCenteredString(font, xpText, x + width / 2, y - 10, 0xDCE6F6);
    }

    private void renderSkillsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 14;
        int y = topPos + 62;
        int w = guiWidth - 28;
        int h = guiHeight - 76;

        Optional<PlayerProgressData> progressOpt = ProgressApi.get(player);
        int playerSkillPoints = progressOpt.map(PlayerProgressData::playerSkillPoints).orElse(0);
        int classSkillPoints = progressOpt.map(PlayerProgressData::classSkillPoints).orElse(0);

        graphics.drawString(font, Component.literal("Skill Tree"), x, y - 10, 0xE8C78D, false);
        graphics.drawString(font, Component.literal("Skill XP Domain: combat/mobs/active play"), x + 84, y - 10, 0xA4C8FF, false);
        graphics.drawString(font, Component.literal("Class XP Domain: guild quests only"), x + 84, y + 2, 0xE8C190, false);
        graphics.drawString(font, Component.literal("Skill Pts: " + playerSkillPoints), x + w - 144, y - 10, 0xA4C8FF, false);
        graphics.drawString(font, Component.literal("Class Pts: " + classSkillPoints), x + w - 144, y + 2, 0xE8C190, false);

        int treeButtonX = x;
        int treeButtonY = y + h - 12;
        for (String treeId : SkillTreeManager.treeIds()) {
            int color = treeId.equals(activeSkillTree) ? 0xFFD6B37A : 0xFF9EA7B6;
            graphics.drawString(font, Component.literal("[" + treeId + "]"), treeButtonX, treeButtonY, color, false);
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
        int currentMana = ManaApi.get(player).map(mana -> (int) Math.floor(mana.currentMana())).orElse(0);
        int maxMana = Math.max(1, ManaApi.get(player).map(mana -> (int) Math.ceil(mana.maxMana())).orElse(stats.maxMana()));

        graphics.drawString(font, Component.literal("Mana: " + currentMana + " / " + maxMana), x, y, 0xCFAEFF, false);
        graphics.drawString(font, Component.literal("Aether: backend telemetry pending"), x + 128, y, 0xB7E8FF, false);
        graphics.drawString(font, Component.literal("Magic Power: " + stats.magicPower()), x, y + 14, 0xCFAEFF, false);
        graphics.drawString(font, Component.literal("Readiness: " + readinessLabel(currentMana, maxMana)), x + 128, y + 14, 0xF0D4A8, false);

        int time = player.tickCount;
        for (int i = 0; i < 8; i++) {
            int sx = x + (i % 4) * 28;
            int sy = y + 38 + (i / 4) * 28;

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

        List<Spell> spells = new ArrayList<>(SpellRegistry.all());
        if (spells.isEmpty()) {
            graphics.drawString(font, Component.literal("No spell data loaded."), x + 132, y + 42, 0xC8CFDB, false);
            return;
        }

        selectedSpellIndex = Mth.clamp(selectedSpellIndex, 0, spells.size() - 1);
        Spell selected = spells.get(selectedSpellIndex);
        int listY = y + 42;
        int listX = x + 132;

        graphics.drawString(font, Component.literal("Spellbook"), listX, listY - 10, 0xE8C78D, false);
        graphics.drawString(font, Component.literal("School: " + schoolLabel(selected.element())), listX, listY + 4, schoolColor(selected.element()), false);
        graphics.drawString(font, Component.literal("Type: " + selected.type().name().toLowerCase(Locale.ROOT)), listX, listY + 16, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Cost: " + selected.manaCost() + " mana"), listX, listY + 28, selected.manaCost() <= currentMana ? 0x8FF3B2 : 0xFF9B9B, false);
        graphics.drawString(font, Component.literal("Cooldown: " + (selected.cooldownTicks() / 20.0F) + "s"), listX + 104, listY + 28, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Range: " + String.format("%.1f", selected.range())), listX, listY + 40, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Radius: " + String.format("%.1f", selected.radius())), listX + 104, listY + 40, 0xC8CFDB, false);

        int rowY = listY + 56;
        int visible = Math.min(5, spells.size());
        int start = Math.max(0, Math.min(selectedSpellIndex - 2, spells.size() - visible));
        for (int i = 0; i < visible; i++) {
            int idx = start + i;
            Spell spell = spells.get(idx);
            int color = idx == selectedSpellIndex ? 0xF7E2B5 : 0xC4CEDD;
            graphics.drawString(font, Component.literal((idx == selectedSpellIndex ? "> " : "  ") + spell.id()), listX, rowY + (i * 12), color, false);
        }

        graphics.drawString(font, Component.literal("Use mouse wheel to browse spells"), listX, rowY + 66, 0x92A7C1, false);
        graphics.drawString(font, Component.literal("Composition UI: awaiting backend support"), listX, rowY + 78, 0x92A7C1, false);
    }

    private void renderDualWieldTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        graphics.drawString(font, Component.literal("Main Hand"), x, y, 0xD8DEE9, false);
        graphics.drawString(font, Component.literal("Offhand"), x + 120, y, 0xD8DEE9, false);

        drawItemSlot(graphics, x, y + 14, main);
        drawItemSlot(graphics, x + 120, y + 14, off);

        if (mouseX >= x && mouseX <= x + 18 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, main, mouseX, mouseY);
        }
        if (mouseX >= x + 120 && mouseX <= x + 138 && mouseY >= y + 14 && mouseY <= y + 32) {
            graphics.renderTooltip(font, off, mouseX, mouseY);
        }

        graphics.drawString(font, Component.literal("Right-click entity: offhand attack"), x, y + 46, 0xB5C3D8, false);
        graphics.drawString(font, Component.literal("Left-click: main hand (vanilla)"), x, y + 60, 0xB5C3D8, false);
    }

    private void drawItemSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        graphics.fill(x, y, x + 18, y + 18, 0xAA1F2432);
        graphics.fill(x + 1, y + 1, x + 17, y + 17, 0x660A0C11);
        graphics.renderItem(stack, x + 1, y + 1);
    }

    private void renderClassTab(GuiGraphics graphics) {
        int x = leftPos + 16;
        int y = topPos + 62;

        Optional<PlayerProgressData> progressOpt = ProgressApi.get(player);
        String currentClassId = progressOpt.map(PlayerProgressData::currentClass).orElse("warrior");
        int classSkillPoints = progressOpt.map(PlayerProgressData::classSkillPoints).orElse(0);
        int playerSkillPoints = progressOpt.map(PlayerProgressData::playerSkillPoints).orElse(0);
        int completedQuests = progressOpt.map(data -> data.completedQuests().size()).orElse(0);

        graphics.drawString(font, Component.literal("Class Progression"), x, y, 0xE3CDA0, false);
        graphics.drawString(font, Component.literal("Current Class: " + currentClassId), x, y + 14, 0xF2E6C9, false);
        graphics.drawString(font, Component.literal("Class Skill Points: " + classSkillPoints), x, y + 26, 0xE8C190, false);
        graphics.drawString(font, Component.literal("Player Skill Points: " + playerSkillPoints), x + 168, y + 26, 0xA4C8FF, false);
        graphics.drawString(font, Component.literal("Completed Guild Quests: " + completedQuests), x, y + 38, 0xD0D8E6, false);

        graphics.drawString(font, Component.literal("XP Domains"), x, y + 56, 0xE8C78D, false);
        graphics.drawString(font, Component.literal("- Skill XP: combat + active gameplay"), x + 8, y + 68, 0xA4C8FF, false);
        graphics.drawString(font, Component.literal("- Class XP: quest completion only"), x + 8, y + 80, 0xE8C190, false);

        graphics.drawString(font, Component.literal("Available Classes"), x, y + 102, 0xE8C78D, false);
        int rowY = y + 114;
        for (PlayerClass klass : ClassRegistry.all()) {
            boolean selected = klass.id().equalsIgnoreCase(currentClassId);
            int color = selected ? 0xF6DEB2 : 0xC8CFDB;
            graphics.drawString(font, Component.literal((selected ? "> " : "  ") + klass.id() + " req lvl " + klass.requiredLevel()), x + 8, rowY, color, false);
            rowY += 11;
            if (rowY > y + 168) {
                break;
            }
        }

        graphics.drawString(font, Component.literal("Quest Pool: " + QuestManager.all().size() + " loaded"), x + 168, y + 102, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Class switching UI: backend-safe wiring pending"), x + 168, y + 114, 0x92A7C1, false);
    }

    private void renderModulesTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        ItemStack main = player.getMainHandItem();
        ItemStack chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);

        graphics.drawString(font, Component.literal("Tech Console"), x, y, 0xE8C78D, false);
        graphics.drawString(font, Component.literal("What powers it: Fuel or network energy"), x, y + 14, 0xB7C8DD, false);
        graphics.drawString(font, Component.literal("Why stalled: missing fuel/input/output space/structure"), x, y + 26, 0xB7C8DD, false);
        graphics.drawString(font, Component.literal("Hold a modular tool or wear modular chest armor."), x, y + 38, 0xC8CFDB, false);
        graphics.drawString(font, Component.literal("Main: " + main.getHoverName().getString()), x, y + 50, 0xD8DEE9, false);
        graphics.drawString(font, Component.literal("Chest: " + chest.getHoverName().getString()), x, y + 62, 0xD8DEE9, false);

        int searchX = x;
        int searchY = y + 76;
        drawActionButton(graphics, searchX, searchY, 190, 16, "Search: " + (moduleSearchQuery.isBlank() ? "<type to filter>" : moduleSearchQuery));
        drawActionButton(graphics, searchX + 194, searchY, 32, 16, "CLR");

        List<ModuleCatalogClientState.ModuleEntry> toolFiltered = filteredModules(ModuleCatalogClientState.toolModules());
        List<ModuleCatalogClientState.ModuleEntry> armorFiltered = filteredModules(ModuleCatalogClientState.armorModules());

        int toolPages = pageCount(toolFiltered.size());
        int armorPages = pageCount(armorFiltered.size());
        toolPage = Math.min(toolPage, Math.max(0, toolPages - 1));
        armorPage = Math.min(armorPage, Math.max(0, armorPages - 1));

        int toolX = x;
        int armorX = x + 188;
        int listTopY = y + 100;

        graphics.drawString(font, Component.literal("Tool Modules"), toolX, y + 94, 0xE0E6F2, false);
        drawActionButton(graphics, toolX + 108, y + 92, 16, 16, "<");
        drawActionButton(graphics, toolX + 126, y + 92, 16, 16, ">");
        graphics.drawString(font, Component.literal((toolPage + 1) + "/" + toolPages), toolX + 146, y + 96, 0xAFC0D8, false);

        graphics.drawString(font, Component.literal("Armor Modules"), armorX, y + 94, 0xE0E6F2, false);
        drawActionButton(graphics, armorX + 108, y + 92, 16, 16, "<");
        drawActionButton(graphics, armorX + 126, y + 92, 16, 16, ">");
        graphics.drawString(font, Component.literal((armorPage + 1) + "/" + armorPages), armorX + 146, y + 96, 0xAFC0D8, false);

        List<ModuleCatalogClientState.ModuleEntry> toolModules = pagedModules(toolFiltered, toolPage);
        List<ModuleCatalogClientState.ModuleEntry> armorModules = pagedModules(armorFiltered, armorPage);

        for (int i = 0; i < toolModules.size(); i++) {
            ModuleCatalogClientState.ModuleEntry entry = toolModules.get(i);
            int rowY = listTopY + (i * 18);
            drawActionButton(graphics, toolX, rowY, 16, 16, "+");
            drawActionButton(graphics, toolX + 18, rowY, 16, 16, "-");
            graphics.drawString(font, Component.literal(entry.id() + " (" + entry.slotCost() + ")"), toolX + 40, rowY + 4, 0xD8DEE9, false);
        }

        for (int i = 0; i < armorModules.size(); i++) {
            ModuleCatalogClientState.ModuleEntry entry = armorModules.get(i);
            int rowY = listTopY + (i * 18);
            drawActionButton(graphics, armorX, rowY, 16, 16, "+");
            drawActionButton(graphics, armorX + 18, rowY, 16, 16, "-");
            graphics.drawString(font, Component.literal(entry.id() + " (" + entry.slotCost() + ")"), armorX + 40, rowY + 4, 0xD8DEE9, false);
        }

        if (toolFiltered.isEmpty() && armorFiltered.isEmpty()) {
            graphics.drawString(font, Component.literal("No modules match current filter or waiting for sync."), x, y + 182, 0xC8CFDB, false);
        }
    }

    private String readinessLabel(int currentMana, int maxMana) {
        if (currentMana <= 0) {
            return "No mana";
        }
        if (currentMana < Math.max(10, maxMana / 4)) {
            return "Low reserves";
        }
        return "Ready";
    }

    private static String schoolLabel(String element) {
        return switch (element == null ? "" : element.toLowerCase(Locale.ROOT)) {
            case "earth" -> "Earth";
            case "fire" -> "Fire";
            case "storm", "lightning" -> "Storm";
            case "water", "ice", "frost" -> "Water/Ice";
            case "void", "shadow" -> "Void";
            default -> "Unaligned";
        };
    }

    private static int schoolColor(String element) {
        return switch (element == null ? "" : element.toLowerCase(Locale.ROOT)) {
            case "earth" -> 0xB9D58D;
            case "fire" -> 0xFFB27F;
            case "storm", "lightning" -> 0x9FC8FF;
            case "water", "ice", "frost" -> 0xA8E6F8;
            case "void", "shadow" -> 0xD2B3FF;
            default -> 0xC8CFDB;
        };
    }

    private void drawActionButton(GuiGraphics graphics, int x, int y, int w, int h, String label) {
        graphics.fill(x, y, x + w, y + h, 0xAA2A3142);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xAA1A1F2A);
        graphics.drawString(font, Component.literal(label), x + 4, y + 4, 0xE6ECF8, false);
    }

    private boolean mouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private boolean handleModulesClick(double mouseX, double mouseY) {
        int x = leftPos + 16;
        int y = topPos + 62;

        int searchY = y + 76;
        if (mouseOver(mouseX, mouseY, x + 194, searchY, 32, 16)) {
            moduleSearchQuery = "";
            toolPage = 0;
            armorPage = 0;
            return true;
        }

        List<ModuleCatalogClientState.ModuleEntry> toolFiltered = filteredModules(ModuleCatalogClientState.toolModules());
        List<ModuleCatalogClientState.ModuleEntry> armorFiltered = filteredModules(ModuleCatalogClientState.armorModules());

        int toolPages = pageCount(toolFiltered.size());
        int armorPages = pageCount(armorFiltered.size());

        int toolX = x;
        int armorX = x + 188;

        if (mouseOver(mouseX, mouseY, toolX + 108, y + 92, 16, 16)) {
            toolPage = Math.max(0, toolPage - 1);
            return true;
        }
        if (mouseOver(mouseX, mouseY, toolX + 126, y + 92, 16, 16)) {
            toolPage = Math.min(Math.max(0, toolPages - 1), toolPage + 1);
            return true;
        }
        if (mouseOver(mouseX, mouseY, armorX + 108, y + 92, 16, 16)) {
            armorPage = Math.max(0, armorPage - 1);
            return true;
        }
        if (mouseOver(mouseX, mouseY, armorX + 126, y + 92, 16, 16)) {
            armorPage = Math.min(Math.max(0, armorPages - 1), armorPage + 1);
            return true;
        }

        List<ModuleCatalogClientState.ModuleEntry> toolModules = pagedModules(toolFiltered, toolPage);
        List<ModuleCatalogClientState.ModuleEntry> armorModules = pagedModules(armorFiltered, armorPage);

        int listTopY = y + 100;
        for (int i = 0; i < toolModules.size(); i++) {
            int rowY = listTopY + (i * 18);
            ModuleCatalogClientState.ModuleEntry entry = toolModules.get(i);
            if (mouseOver(mouseX, mouseY, toolX, rowY, 16, 16)) {
                ModNetwork.CHANNEL.sendToServer(new InstallModuleC2SPacket(entry.id(), "MAIN_HAND"));
                return true;
            }
            if (mouseOver(mouseX, mouseY, toolX + 18, rowY, 16, 16)) {
                ModNetwork.CHANNEL.sendToServer(new RemoveModuleC2SPacket(entry.id(), "MAIN_HAND"));
                return true;
            }
        }

        for (int i = 0; i < armorModules.size(); i++) {
            int rowY = listTopY + (i * 18);
            ModuleCatalogClientState.ModuleEntry entry = armorModules.get(i);
            if (mouseOver(mouseX, mouseY, armorX, rowY, 16, 16)) {
                ModNetwork.CHANNEL.sendToServer(new InstallModuleC2SPacket(entry.id(), "CHESTPLATE"));
                return true;
            }
            if (mouseOver(mouseX, mouseY, armorX + 18, rowY, 16, 16)) {
                ModNetwork.CHANNEL.sendToServer(new RemoveModuleC2SPacket(entry.id(), "CHESTPLATE"));
                return true;
            }
        }

        return false;
    }

    private List<ModuleCatalogClientState.ModuleEntry> filteredModules(List<ModuleCatalogClientState.ModuleEntry> modules) {
        if (moduleSearchQuery.isBlank()) {
            return modules;
        }

        String query = moduleSearchQuery.toLowerCase();
        List<ModuleCatalogClientState.ModuleEntry> filtered = new ArrayList<>();
        for (ModuleCatalogClientState.ModuleEntry entry : modules) {
            if (entry.id().toLowerCase().contains(query) || entry.requiredSkillNode().toLowerCase().contains(query)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private int pageCount(int totalItems) {
        return Math.max(1, (int) Math.ceil(totalItems / (double) MODULES_PAGE_SIZE));
    }

    private List<ModuleCatalogClientState.ModuleEntry> pagedModules(List<ModuleCatalogClientState.ModuleEntry> filtered, int page) {
        int start = Math.max(0, page * MODULES_PAGE_SIZE);
        int end = Math.min(filtered.size(), start + MODULES_PAGE_SIZE);
        if (start >= end) {
            return List.of();
        }
        return filtered.subList(start, end);
    }

    private Optional<PlayerStatsCapability> getStats() {
        return PlayerStatsApi.get(player);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == ExtremePlayerTabs.Tab.SKILLS && button == InputConstants.MOUSE_BUTTON_LEFT) {
            int panelX = leftPos + 14;
            int panelY = topPos + 62;
            int panelW = guiWidth - 28;
            int panelH = guiHeight - 76;
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
        if (activeTab == ExtremePlayerTabs.Tab.MODULES && button == InputConstants.MOUSE_BUTTON_LEFT && handleModulesClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (activeTab == ExtremePlayerTabs.Tab.MODULES && !Character.isISOControl(codePoint)) {
            if (moduleSearchQuery.length() < 32) {
                moduleSearchQuery = moduleSearchQuery + codePoint;
                toolPage = 0;
                armorPage = 0;
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTab == ExtremePlayerTabs.Tab.MODULES && keyCode == InputConstants.KEY_BACKSPACE) {
            if (!moduleSearchQuery.isEmpty()) {
                moduleSearchQuery = moduleSearchQuery.substring(0, moduleSearchQuery.length() - 1);
                toolPage = 0;
                armorPage = 0;
                return true;
            }
        }
        if (activeTab == ExtremePlayerTabs.Tab.MAGIC) {
            if (keyCode == InputConstants.KEY_UP) {
                selectedSpellIndex = Math.max(0, selectedSpellIndex - 1);
                return true;
            }
            if (keyCode == InputConstants.KEY_DOWN) {
                selectedSpellIndex++;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeTab == ExtremePlayerTabs.Tab.MAGIC) {
            selectedSpellIndex = Math.max(0, selectedSpellIndex - (delta > 0 ? 1 : -1));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
