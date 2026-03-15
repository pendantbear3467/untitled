package com.extremecraft.progression.skilltree;

import com.extremecraft.client.gui.BaseExtremeScreen;
import com.extremecraft.client.gui.theme.ECGuiPrimitives;
import com.extremecraft.client.gui.theme.ECGuiTheme;
import com.extremecraft.core.ECConstants;

import com.extremecraft.progression.capability.ProgressApi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SkillTreeScreen extends BaseExtremeScreen {
    private static final ResourceLocation NODE_LOCKED = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_node_locked.png");
    private static final ResourceLocation NODE_UNLOCKED = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_node_unlocked.png");
    private static final ResourceLocation NODE_HOVER = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_node_hover.png");

    private static final ResourceLocation CONNECTION_LOCKED = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_connection_locked.png");
    private static final ResourceLocation CONNECTION_AVAILABLE = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_connection_unlocked.png");
    private static final ResourceLocation CONNECTION_ACTIVE = new ResourceLocation(ECConstants.MODID, "textures/gui/skill_connection_active.png");

    private static final ResourceLocation ICON_ATTACK = new ResourceLocation(ECConstants.MODID, "textures/gui/skills/attack.png");
    private static final ResourceLocation ICON_MINING = new ResourceLocation(ECConstants.MODID, "textures/gui/skills/mining.png");
    private static final ResourceLocation ICON_DEFENSE = new ResourceLocation(ECConstants.MODID, "textures/gui/skills/defense.png");
    private static final ResourceLocation ICON_MAGIC = new ResourceLocation(ECConstants.MODID, "textures/gui/skills/magic.png");

    private static final int NODE_SIZE = 26;

    private String activeTreeId = "warrior";
    private SkillNode hoveredNode;

    private double panX;
    private double panY;
    private double zoom = 1.0D;

    private boolean dragging;
    private double dragLastX;
    private double dragLastY;
    private Button unlockButton;

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
        this.panelWidth = 332;
        this.panelHeight = 228;
    }

    @Override
    protected void init() {
        super.init();
        SkillTree defaultTree = SkillTreeRegistry.get(activeTreeId);
        if (defaultTree == null) {
            SkillTree first = SkillTreeRegistry.first();
            if (first != null) {
                activeTreeId = first.id();
            }
        }

        unlockButton = addRenderableWidget(Button.builder(Component.literal("Unlock Node"), button -> {
            if (hoveredNode != null) {
                SkillTreeClientActions.requestUnlock(activeTreeId, hoveredNode.id());
            }
        }).bounds(panelLeft + panelWidth - 110, panelTop + 24, 94, 18).build());
    }

    @Override
    protected void drawContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hoveredNode = null;
        SkillTree tree = SkillTreeRegistry.get(activeTreeId);
        if (tree == null) {
            guiGraphics.drawString(this.font, Component.literal("No skill tree JSON loaded."), panelLeft + 14, panelTop + 14, 0xEADFC8, false);
            if (unlockButton != null) {
                unlockButton.active = false;
            }
            return;
        }

        drawHeader(guiGraphics);
        drawTreeBounds(guiGraphics);
        drawConnections(guiGraphics, tree);
        drawNodes(guiGraphics, tree, mouseX, mouseY, partialTick);

        if (unlockButton != null) {
            unlockButton.active = hoveredNode != null && canUnlockNode(hoveredNode);
        }
    }

    @Override
    protected void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (hoveredNode == null) {
            return;
        }

        int skillPoints = Minecraft.getInstance().player == null ? 0 : ProgressApi.get(Minecraft.getInstance().player).map(data -> data.playerSkillPoints()).orElse(0);
        int level = Minecraft.getInstance().player == null ? 0 : ProgressApi.get(Minecraft.getInstance().player).map(data -> data.level()).orElse(0);
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(hoveredNode.resolvedDisplayName()));
        tooltip.add(Component.literal("Purpose: " + hoveredNode.resolvedDescription()));
        tooltip.add(Component.literal("Effect: " + hoveredNode.bonusText()));
        tooltip.add(Component.literal("Cost: " + hoveredNode.cost() + " skill point(s)"));
        tooltip.add(Component.literal("Required Level: " + hoveredNode.requiredLevel()));
        if (!hoveredNode.requiredNodes().isEmpty()) {
            tooltip.add(Component.literal("Requirements: " + String.join(", ", hoveredNode.requiredNodes())));
        }
        if (isNodeUnlocked(hoveredNode.id())) {
            tooltip.add(Component.literal("State: UNLOCKED"));
        } else if (canUnlockNode(hoveredNode)) {
            tooltip.add(Component.literal("State: UNLOCKABLE"));
        } else if (level < hoveredNode.requiredLevel()) {
            tooltip.add(Component.literal("Blocked: requires level " + hoveredNode.requiredLevel()));
        } else if (skillPoints < hoveredNode.cost()) {
            tooltip.add(Component.literal("Blocked: not enough skill points"));
        } else {
            tooltip.add(Component.literal("Blocked: prerequisite node missing"));
        }
        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        int skillPoints = Minecraft.getInstance().player == null ? 0 : ProgressApi.get(Minecraft.getInstance().player).map(data -> data.playerSkillPoints()).orElse(0);
        ECGuiPrimitives.drawSectionHeader(guiGraphics, this.font, Component.literal("Skill Tree: " + activeTreeId.toUpperCase(Locale.ROOT)), panelLeft + 16, panelTop + 14, 160, ECGuiTheme.ACCENT_VIOLET);
        ECGuiPrimitives.drawStatusChip(guiGraphics, this.font, panelLeft + 16, panelTop + 26, Component.literal("Points " + skillPoints), ECGuiTheme.ACCENT_VIOLET);
        guiGraphics.drawString(this.font, Component.literal("Drag=Pan  Scroll=Zoom"), panelLeft + 132, panelTop + 14, ECGuiTheme.TEXT_MUTED, false);
    }

    private void drawTreeBounds(GuiGraphics guiGraphics) {
        guiGraphics.fill(treeLeft(), treeTop(), treeRight(), treeBottom(), 0x7A101420);
        guiGraphics.fillGradient(treeLeft(), treeTop(), treeRight(), treeBottom(), 0x2200729C, 0x220D1230);
        guiGraphics.fill(treeLeft(), treeTop(), treeRight(), treeTop() + 1, 0x664EDCFF);
        guiGraphics.fill(treeLeft(), treeBottom() - 1, treeRight(), treeBottom(), 0x443D5068);
    }

    private void drawConnections(GuiGraphics guiGraphics, SkillTree tree) {
        for (SkillNode node : tree.nodes()) {
            for (String requirement : node.requiredNodes()) {
                SkillNode requiredNode = tree.getNode(requirement);
                if (requiredNode == null) {
                    continue;
                }

                boolean requiredUnlocked = isNodeUnlocked(requiredNode.id());
                boolean unlocked = isNodeUnlocked(node.id());
                ConnectionState state = unlocked && requiredUnlocked ? ConnectionState.ACTIVE
                        : requiredUnlocked ? ConnectionState.AVAILABLE
                        : ConnectionState.LOCKED;

                int x0 = toScreenX(requiredNode.x()) + (NODE_SIZE / 2);
                int y0 = toScreenY(requiredNode.y()) + (NODE_SIZE / 2);
                int x1 = toScreenX(node.x()) + (NODE_SIZE / 2);
                int y1 = toScreenY(node.y()) + (NODE_SIZE / 2);
                drawConnection(guiGraphics, x0, y0, x1, y1, state);
            }
        }
    }

    private void drawNodes(GuiGraphics guiGraphics, SkillTree tree, int mouseX, int mouseY, float partialTick) {
        for (SkillNode node : tree.nodes()) {
            int x = toScreenX(node.x());
            int y = toScreenY(node.y());
            boolean hovered = mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE;
            boolean unlocked = isNodeUnlocked(node.id());

            if (hovered) {
                hoveredNode = node;
            }

            ResourceLocation baseTexture = hovered ? NODE_HOVER : (unlocked ? NODE_UNLOCKED : NODE_LOCKED);
            guiGraphics.blit(baseTexture, x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);

            if (unlocked) {
                float pulse = 0.45F + (float) (Math.sin((Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0) / 6.0D + node.x()) * 0.25F);
                int glow = ((int) (pulse * 255.0F) << 24) | 0x4B88FF;
                guiGraphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, glow);
            }

            ResourceLocation icon = iconForNode(node);
            guiGraphics.blit(icon, x + 5, y + 5, 0, 0, 16, 16, 16, 16);

            if (hovered || canUnlockNode(node)) {
                int ringColor = hovered ? 0x99EAC270 : 0x5580A9FF;
                guiGraphics.fill(x - 1, y - 1, x + NODE_SIZE + 1, y, ringColor);
                guiGraphics.fill(x - 1, y + NODE_SIZE, x + NODE_SIZE + 1, y + NODE_SIZE + 1, ringColor);
                guiGraphics.fill(x - 1, y, x, y + NODE_SIZE, ringColor);
                guiGraphics.fill(x + NODE_SIZE, y, x + NODE_SIZE + 1, y + NODE_SIZE, ringColor);
            }
        }
    }

    private void drawConnection(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, ConnectionState state) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            return;
        }

        ResourceLocation texture = switch (state) {
            case LOCKED -> CONNECTION_LOCKED;
            case AVAILABLE -> CONNECTION_AVAILABLE;
            case ACTIVE -> CONNECTION_ACTIVE;
        };

        for (int i = 0; i <= steps; i += 2) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            guiGraphics.blit(texture, x - 1, y - 1, 0, 0, 3, 3, 3, 3);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double previousZoom = zoom;
        zoom = Math.max(0.45D, Math.min(2.8D, zoom + (delta > 0 ? 0.1D : -0.1D)));

        double worldX = (mouseX - treeCenterX()) / previousZoom - panX;
        double worldY = (mouseY - treeCenterY()) / previousZoom - panY;
        panX = (mouseX - treeCenterX()) / zoom - worldX;
        panY = (mouseY - treeCenterY()) / zoom - worldY;
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            SkillTree tree = SkillTreeRegistry.get(activeTreeId);
            if (tree != null) {
                for (SkillNode node : tree.nodes()) {
                    int x = toScreenX(node.x());
                    int y = toScreenY(node.y());
                    if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE) {
                        SkillTreeClientActions.requestUnlock(activeTreeId, node.id());
                        return true;
                    }
                }
            }

            dragging = true;
            dragLastX = mouseX;
            dragLastY = mouseY;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging && button == 0) {
            panX += (mouseX - dragLastX) / zoom;
            panY += (mouseY - dragLastY) / zoom;
            dragLastX = mouseX;
            dragLastY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean isNodeUnlocked(String nodeId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        return PlayerSkillTreeApi.get(mc.player)
                .map(data -> data.isUnlocked(activeTreeId, nodeId))
                .orElse(false);
    }

    private boolean canUnlockNode(SkillNode node) {
        if (isNodeUnlocked(node.id())) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        int playerLevel = mc.player == null ? 0 : ProgressApi.get(mc.player)
                .map(data -> data.level())
                .orElse(0);
        int playerSkillPoints = mc.player == null ? 0 : ProgressApi.get(mc.player)
                .map(data -> data.playerSkillPoints())
                .orElse(0);
        if (playerLevel < node.requiredLevel()) {
            return false;
        }
        if (playerSkillPoints < node.cost()) {
            return false;
        }
        if (node.requiredNodes().isEmpty()) {
            return true;
        }
        for (String req : node.requiredNodes()) {
            if (!isNodeUnlocked(req)) {
                return false;
            }
        }
        return true;
    }

    private ResourceLocation iconForNode(SkillNode node) {
        String iconPath = node.resolvedIconPath();
        if (!iconPath.isBlank()) {
            if (iconPath.contains(":")) {
                ResourceLocation parsed = ResourceLocation.tryParse(iconPath);
                if (parsed != null) {
                    return parsed;
                }
            } else {
                return new ResourceLocation(ECConstants.MODID, iconPath);
            }
        }

        String key = (node.id() + " " + node.bonusText()).toLowerCase(Locale.ROOT);
        if (key.contains("mine") || key.contains("ore") || key.contains("dig")) {
            return ICON_MINING;
        }
        if (key.contains("magic") || key.contains("mana") || key.contains("arcane")) {
            return ICON_MAGIC;
        }
        if (key.contains("defense") || key.contains("armor") || key.contains("health")) {
            return ICON_DEFENSE;
        }
        return ICON_ATTACK;
    }

    private int treeLeft() {
        return panelLeft + 16;
    }

    private int treeTop() {
        return panelTop + 48;
    }

    private int treeRight() {
        return panelLeft + panelWidth - 16;
    }

    private int treeBottom() {
        return panelTop + panelHeight - 14;
    }

    private int treeCenterX() {
        return (treeLeft() + treeRight()) / 2;
    }

    private int treeCenterY() {
        return (treeTop() + treeBottom()) / 2;
    }

    private int toScreenX(int worldX) {
        return (int) Math.round(treeCenterX() + (worldX + panX) * zoom);
    }

    private int toScreenY(int worldY) {
        return (int) Math.round(treeCenterY() + (worldY + panY) * zoom);
    }

    private enum ConnectionState {
        LOCKED,
        AVAILABLE,
        ACTIVE
    }
}

