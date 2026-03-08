package com.extremecraft.client.gui.player;

import com.extremecraft.config.DwConfig;
import com.extremecraft.core.ECConstants;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import com.extremecraft.progression.skilltree.SkillTreeClientActions;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import com.extremecraft.progression.skilltree.service.SkillNodeStateService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class SkillTreeScreenPanel {
    private static final int NODE_SIZE = 18;

    private final Supplier<Optional<PlayerStatsCapability>> statsSupplier;
    private final Map<String, NodeUiCache> nodeUiCache = new HashMap<>();
    private final List<Component> tooltipBuffer = new ArrayList<>(6);

    public SkillTreeScreenPanel(Supplier<Optional<PlayerStatsCapability>> statsSupplier) {
        this.statsSupplier = statsSupplier;
    }

    public void render(GuiGraphics graphics, Font font, int panelLeft, int panelTop, int panelWidth, int panelHeight,
                       String activeTreeId, int mouseX, int mouseY) {
        List<SkillNode> nodes = SkillTreeManager.nodesForTree(activeTreeId);
        if (nodes.isEmpty()) {
            graphics.drawString(font, Component.literal("No nodes in tree."), panelLeft + 8, panelTop + 8, 0xD5D9E2, false);
            return;
        }

        float zoom = configuredZoom();
        int centerX = panelLeft + panelWidth / 2;
        int centerY = panelTop + panelHeight / 2;

        renderConnections(graphics, activeTreeId, centerX, centerY, zoom);

        NodeUiCache hovered = null;
        for (SkillNode node : nodes) {
            NodeUiCache ui = uiCache(node);

            int x = nodeScreenX(centerX, node.x(), zoom);
            int y = nodeScreenY(centerY, node.y(), zoom);
            SkillNodeStateService.NodeState state = nodeState(node);
            boolean unlocked = state == SkillNodeStateService.NodeState.UNLOCKED;
            boolean canUnlock = state == SkillNodeStateService.NodeState.UNLOCKABLE;

            int fill = unlocked ? 0xCC5AA6FF : (canUnlock ? 0xAA6B5FA3 : 0xAA2E3140);
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, fill);

            drawNodeIcon(graphics, ui, x + 1, y + 1);

            if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE) {
                hovered = ui.withState(state);
            }
        }

        if (hovered != null && DwConfig.CLIENT.showSkillNodeTooltips.get()) {
            tooltipBuffer.clear();
            tooltipBuffer.add(hovered.displayName);
            tooltipBuffer.add(hovered.stateLine);
            tooltipBuffer.add(hovered.bonusText);
            tooltipBuffer.add(hovered.costLine);
            if (!hovered.requirementLine.getString().isBlank()) {
                tooltipBuffer.add(hovered.requirementLine);
            }
            tooltipBuffer.add(hovered.nextUnlockLine);
            graphics.renderTooltip(font, tooltipBuffer, Optional.empty(), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(int panelLeft, int panelTop, int panelWidth, int panelHeight,
                                String activeTreeId, double mouseX, double mouseY) {
        List<SkillNode> nodes = SkillTreeManager.nodesForTree(activeTreeId);
        if (nodes.isEmpty()) {
            return false;
        }

        float zoom = configuredZoom();
        int centerX = panelLeft + panelWidth / 2;
        int centerY = panelTop + panelHeight / 2;

        for (SkillNode node : nodes) {
            int x = nodeScreenX(centerX, node.x(), zoom);
            int y = nodeScreenY(centerY, node.y(), zoom);

            if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE
                    && nodeState(node) == SkillNodeStateService.NodeState.UNLOCKABLE) {
                SkillTreeClientActions.requestUnlock(activeTreeId, node.id());
                return true;
            }
        }

        return false;
    }

    private float configuredZoom() {
        return (float) Mth.clamp(DwConfig.CLIENT.skillTreeZoom.get(), 0.50D, 2.00D);
    }

    private int nodeScreenX(int centerX, int nodeX, float zoom) {
        return centerX + Math.round(nodeX * zoom) - NODE_SIZE / 2;
    }

    private int nodeScreenY(int centerY, int nodeY, float zoom) {
        return centerY + Math.round(nodeY * zoom) - NODE_SIZE / 2;
    }

    private void renderConnections(GuiGraphics graphics, String activeTreeId, int centerX, int centerY, float zoom) {
        for (SkillTreeManager.Connection connection : SkillTreeManager.connectionsForTree(activeTreeId)) {
            SkillNode from = SkillTreeManager.getNode(connection.from());
            SkillNode to = SkillTreeManager.getNode(connection.to());
            if (from == null || to == null) {
                continue;
            }

            int x0 = centerX + Math.round(from.x() * zoom);
            int y0 = centerY + Math.round(from.y() * zoom);
            int x1 = centerX + Math.round(to.x() * zoom);
            int y1 = centerY + Math.round(to.y() * zoom);
            drawLine(graphics, x0, y0, x1, y1, 0x885E6D83);
        }
    }

    private void drawNodeIcon(GuiGraphics graphics, NodeUiCache ui, int x, int y) {
        if (ui.iconPresent) {
            graphics.blit(ui.icon, x, y, 0, 0, 16, 16, 16, 16);
        } else {
            graphics.fill(x, y, x + 16, y + 16, 0xFF394250);
        }
    }

    private NodeUiCache uiCache(SkillNode node) {
        return nodeUiCache.computeIfAbsent(node.id(), key -> {
            ResourceLocation location = ResourceLocation.tryParse(ECConstants.MODID + ":" + node.resolvedIconPath());
            boolean present = location != null && Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
            return new NodeUiCache(
                    location,
                    present,
                    Component.literal(node.resolvedDisplayName()),
                    Component.literal("State: Locked"),
                    Component.literal(node.resolvedDescription()),
                    Component.literal("Cost: " + node.cost() + " skill point(s)"),
                    buildRequirementLine(node),
                    Component.literal("Next unlock: Spend skill points in this tree")
            );
        });
    }

    private static Component buildRequirementLine(SkillNode node) {
        StringBuilder builder = new StringBuilder();
        if (node.requiredLevel() > 1) {
            builder.append("Requires level ").append(node.requiredLevel());
        }
        if (!node.requiredNodes().isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("Requires nodes: ").append(String.join(", ", node.requiredNodes()));
        }
        return Component.literal(builder.toString());
    }

    private SkillNodeStateService.NodeState nodeState(SkillNode node) {
        Optional<PlayerStatsCapability> statsOpt = statsSupplier.get();
        return statsOpt
            .map(stats -> SkillNodeStateService.stateFor(stats, node))
            .orElse(SkillNodeStateService.NodeState.LOCKED);
    }

    private void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps <= 0) {
            return;
        }

        for (int i = 0; i <= steps; i++) {
            int x = x0 + (x1 - x0) * i / steps;
            int y = y0 + (y1 - y0) * i / steps;
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    private record NodeUiCache(
            ResourceLocation icon,
            boolean iconPresent,
            Component displayName,
            Component stateLine,
            Component bonusText,
            Component costLine,
            Component requirementLine,
            Component nextUnlockLine
    ) {
        private NodeUiCache withState(SkillNodeStateService.NodeState state) {
            String status = switch (state) {
                case UNLOCKED -> "State: Unlocked";
                case UNLOCKABLE -> "State: Ready to unlock";
                case LOCKED -> "State: Locked";
            };

            String next = switch (state) {
                case UNLOCKED -> "Next unlock: Follow outgoing links";
                case UNLOCKABLE -> "Next unlock: Left-click this node";
                case LOCKED -> "Next unlock: Meet requirements and earn points";
            };

            return new NodeUiCache(icon, iconPresent, displayName, Component.literal(status), bonusText, costLine, requirementLine, Component.literal(next));
        }
    }
}

