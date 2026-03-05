package com.extremecraft.progression.skilltree;

import com.extremecraft.core.ECConstants;
import com.extremecraft.net.DwNetwork;
import com.extremecraft.progression.capability.ProgressApi;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class SkillTreeScreen extends Screen {
    private static final ResourceLocation NODE_LOCKED = new ResourceLocation(ECConstants.MODID, "textures/gui/node_locked.png");
    private static final ResourceLocation NODE_UNLOCKED = new ResourceLocation(ECConstants.MODID, "textures/gui/node_unlocked.png");
    private static final ResourceLocation NODE_HOVER = new ResourceLocation(ECConstants.MODID, "textures/gui/node_hover.png");

    private static final int NODE_SIZE = 16;

    private String activeTreeId = "warrior";
    private SkillNode hoveredNode;

    private double panX;
    private double panY;
    private double zoom = 1.0D;

    private boolean dragging;
    private double dragLastX;
    private double dragLastY;

    public SkillTreeScreen() {
        super(Component.literal("Skill Tree"));
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
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);

        hoveredNode = null;
        SkillTree tree = SkillTreeRegistry.get(activeTreeId);
        if (tree == null) {
            guiGraphics.drawString(this.font, Component.literal("No skill tree JSON loaded."), 14, 14, 0xFFFFFF, false);
            return;
        }

        drawHeader(guiGraphics);
        drawConnections(guiGraphics, tree);
        drawNodes(guiGraphics, tree, mouseX, mouseY);
        drawTooltips(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawHeader(GuiGraphics guiGraphics) {
        guiGraphics.drawString(this.font, Component.literal("Skill Tree: " + activeTreeId), 12, 10, 0xFFFFFF, false);
        int skillPoints = Minecraft.getInstance().player == null ? 0 : ProgressApi.get(Minecraft.getInstance().player).map(data -> data.playerSkillPoints()).orElse(0);
        guiGraphics.drawString(this.font, Component.literal("Points: " + skillPoints), 12, 24, 0xE0E0E0, false);
        guiGraphics.drawString(this.font, Component.literal("Scroll = Zoom | Drag = Pan | Click = Unlock"), 12, 38, 0xB0B0B0, false);
    }

    private void drawConnections(GuiGraphics guiGraphics, SkillTree tree) {
        for (SkillNode node : tree.nodes()) {
            for (String requirement : node.requiredNodes()) {
                SkillNode requiredNode = tree.getNode(requirement);
                if (requiredNode == null) {
                    continue;
                }

                int x0 = toScreenX(requiredNode.x()) + (NODE_SIZE / 2);
                int y0 = toScreenY(requiredNode.y()) + (NODE_SIZE / 2);
                int x1 = toScreenX(node.x()) + (NODE_SIZE / 2);
                int y1 = toScreenY(node.y()) + (NODE_SIZE / 2);
                drawLine(guiGraphics, x0, y0, x1, y1, 0x70FFFFFF);
            }
        }
    }

    private void drawNodes(GuiGraphics guiGraphics, SkillTree tree, int mouseX, int mouseY) {
        for (SkillNode node : tree.nodes()) {
            int x = toScreenX(node.x());
            int y = toScreenY(node.y());
            boolean hovered = mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE;
            boolean unlocked = isNodeUnlocked(node.id());

            ResourceLocation texture = unlocked ? NODE_UNLOCKED : NODE_LOCKED;
            if (hovered) {
                texture = NODE_HOVER;
                hoveredNode = node;
            }

            // Fill first so the node remains visible even if textures are missing from a pack.
            guiGraphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, unlocked ? 0xAA3B7A3E : 0xAA4A4A4A);
            RenderSystem.enableBlend();
            guiGraphics.blit(texture, x, y, 0, 0, NODE_SIZE, NODE_SIZE, NODE_SIZE, NODE_SIZE);
            guiGraphics.drawString(this.font, Component.literal(String.valueOf(node.cost())), x + 5, y + 5, 0xFFFFFF, false);
        }
    }

    private void drawTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredNode == null) {
            return;
        }

        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal(hoveredNode.id()));
        tooltip.add(Component.literal("Cost: " + hoveredNode.cost()));
        if (!hoveredNode.requiredNodes().isEmpty()) {
            tooltip.add(Component.literal("Requires: " + String.join(", ", hoveredNode.requiredNodes())));
        }
        if (!hoveredNode.bonusText().isBlank()) {
            tooltip.add(Component.literal(hoveredNode.bonusText()));
        }
        guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        double previousZoom = zoom;
        zoom = Math.max(0.45D, Math.min(2.8D, zoom + (delta > 0 ? 0.1D : -0.1D)));

        // Keep cursor focus stable while zooming.
        double worldX = (mouseX - (this.width / 2.0D)) / previousZoom - panX;
        double worldY = (mouseY - (this.height / 2.0D)) / previousZoom - panY;
        panX = (mouseX - (this.width / 2.0D)) / zoom - worldX;
        panY = (mouseY - (this.height / 2.0D)) / zoom - worldY;
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
                        DwNetwork.sendToServer(new UnlockSkillNodeC2S(activeTreeId, node.id()));
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

    private int toScreenX(int worldX) {
        return (int) Math.round((this.width / 2.0D) + (worldX + panX) * zoom);
    }

    private int toScreenY(int worldY) {
        return (int) Math.round((this.height / 2.0D) + (worldY + panY) * zoom);
    }

    private void drawLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0) {
            guiGraphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }
}
