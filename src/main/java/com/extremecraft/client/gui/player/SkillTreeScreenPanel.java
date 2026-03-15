package com.extremecraft.client.gui.player;

import com.extremecraft.client.gui.theme.ECGuiTheme;
import com.extremecraft.config.DwConfig;
import com.extremecraft.core.ECConstants;
import com.extremecraft.progression.PlayerProgressData;
import com.extremecraft.progression.capability.ProgressApi;
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
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public class SkillTreeScreenPanel {
    private static final int NODE_SIZE = 20;

    private final Player player;
    private final Supplier<Optional<PlayerStatsCapability>> statsSupplier;
    private final Map<String, NodeUiCache> nodeUiCache = new HashMap<>();
    private final List<Component> tooltipBuffer = new ArrayList<>(6);

    public SkillTreeScreenPanel(Player player, Supplier<Optional<PlayerStatsCapability>> statsSupplier) {
        this.player = player;
        this.statsSupplier = statsSupplier;
    }

    public void render(GuiGraphics graphics, Font font, int panelLeft, int panelTop, int panelWidth, int panelHeight,
                       String activeTreeId, int mouseX, int mouseY) {
        List<SkillNode> nodes = SkillTreeManager.nodesForTree(activeTreeId);
        if (nodes.isEmpty()) {
            graphics.drawString(font, Component.literal("No nodes in tree."), panelLeft + 8, panelTop + 8, ECGuiTheme.TEXT_SECONDARY, false);
            return;
        }

        float zoom = configuredZoom();
        int centerX = panelLeft + panelWidth / 2;
        int centerY = panelTop + panelHeight / 2;
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0x6E101722);
        graphics.fillGradient(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0x22004266, 0x220D1022);

        renderConnections(graphics, activeTreeId, centerX, centerY, zoom);

        NodeUiCache hovered = null;
        for (SkillNode node : nodes) {
            NodeUiCache ui = uiCache(node);

            int x = nodeScreenX(centerX, node.x(), zoom);
            int y = nodeScreenY(centerY, node.y(), zoom);
            SkillNodeStateService.NodeState state = nodeState(node);
            boolean unlocked = state == SkillNodeStateService.NodeState.UNLOCKED;
            boolean canUnlock = state == SkillNodeStateService.NodeState.UNLOCKABLE;
            boolean blocked = state == SkillNodeStateService.NodeState.LOCKED;
            int frameColor = unlocked ? ECGuiTheme.STATE_READY : (canUnlock ? ECGuiTheme.ACCENT_AMBER : ECGuiTheme.BORDER_SOFT);
            int fillColor = unlocked ? 0xAA133426 : (canUnlock ? 0xAA332714 : 0xAA1A1F2B);

            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, frameColor);
            graphics.fill(x + 1, y + 1, x + NODE_SIZE - 1, y + NODE_SIZE - 1, fillColor);

            if (canUnlock) {
                graphics.fill(x + 2, y + 2, x + NODE_SIZE - 2, y + 4, 0xAAFFD19D);
            } else if (unlocked) {
                graphics.fill(x + 2, y + 2, x + NODE_SIZE - 2, y + 4, 0xAA85F2AC);
            }

            drawNodeIcon(graphics, ui, x + 1, y + 1);

            if (blocked) {
                graphics.drawString(font, Component.literal("!"), x + NODE_SIZE - 6, y + 2, ECGuiTheme.STATE_WARN, false);
            }

            if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE) {
                hovered = ui.withState(state, lockReason(node, state));
                graphics.fill(x - 2, y - 2, x + NODE_SIZE + 2, y - 1, 0xAA4EDCFF);
                graphics.fill(x - 2, y + NODE_SIZE + 1, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0xAA4EDCFF);
                graphics.fill(x - 2, y - 2, x - 1, y + NODE_SIZE + 2, 0xAA4EDCFF);
                graphics.fill(x + NODE_SIZE + 1, y - 2, x + NODE_SIZE + 2, y + NODE_SIZE + 2, 0xAA4EDCFF);
            }
        }

        if (hovered != null && DwConfig.CLIENT.showSkillNodeTooltips.get()) {
            tooltipBuffer.clear();
            tooltipBuffer.add(hovered.displayName);
            tooltipBuffer.add(hovered.purposeLine);
            tooltipBuffer.add(hovered.stateLine);
            tooltipBuffer.add(hovered.effectLine);
            tooltipBuffer.add(hovered.costLine);
            if (!hovered.requirementLine.getString().isBlank()) {
                tooltipBuffer.add(hovered.requirementLine);
            }
            tooltipBuffer.add(hovered.lockReasonLine);
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
            SkillNodeStateService.NodeState fromState = nodeState(from);
            SkillNodeStateService.NodeState toState = nodeState(to);
            int color = toState == SkillNodeStateService.NodeState.UNLOCKED && fromState == SkillNodeStateService.NodeState.UNLOCKED
                    ? 0xCC67E7A0
                    : toState == SkillNodeStateService.NodeState.UNLOCKABLE && fromState == SkillNodeStateService.NodeState.UNLOCKED
                    ? 0xCCFFD097
                    : 0x66404B60;
            drawLine(graphics, x0, y0, x1, y1, color);
        }
    }

    private void drawNodeIcon(GuiGraphics graphics, NodeUiCache ui, int x, int y) {
        if (ui.iconPresent) {
            graphics.blit(ui.icon, x + 1, y + 1, 0, 0, 16, 16, 16, 16);
        } else {
            graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF394250);
        }
    }

    private NodeUiCache uiCache(SkillNode node) {
        return nodeUiCache.computeIfAbsent(node.id(), key -> {
            ResourceLocation location = ResourceLocation.tryParse(ECConstants.MODID + ":" + node.resolvedIconPath());
            boolean present = location != null && Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
            return new NodeUiCache(
                    node.id(),
                    location,
                    present,
                    Component.literal(node.resolvedDisplayName()),
                    Component.literal("Purpose: " + node.resolvedDescription()),
                    Component.literal("State: Locked"),
                    Component.literal("Effect: " + node.bonusText()),
                    Component.literal("Cost: " + node.cost() + " skill point(s)"),
                    buildRequirementLine(node),
                    Component.literal("Blocked: requirements not met")
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
            .map(stats -> SkillNodeStateService.stateFor(player, stats, node))
            .orElse(SkillNodeStateService.NodeState.LOCKED);
    }

    private Component lockReason(SkillNode node, SkillNodeStateService.NodeState state) {
        Optional<PlayerStatsCapability> statsOpt = statsSupplier.get();
        if (statsOpt.isEmpty()) {
            return Component.literal("Blocked: waiting for sync");
        }

        PlayerStatsCapability stats = statsOpt.get();
        int level = ProgressApi.get(player).map(PlayerProgressData::level).orElse(stats.level());
        int points = ProgressApi.get(player).map(PlayerProgressData::playerSkillPoints).orElse(stats.skillPoints());

        if (state == SkillNodeStateService.NodeState.UNLOCKED) {
            return Component.literal("Blocked: already unlocked");
        }
        if (state == SkillNodeStateService.NodeState.UNLOCKABLE) {
            return Component.literal("Ready: left-click to unlock");
        }
        if (level < node.requiredLevel()) {
            return Component.literal("Blocked: requires level " + node.requiredLevel());
        }
        if (points < node.cost()) {
            return Component.literal("Blocked: need " + node.cost() + " skill points");
        }

        List<String> missing = new ArrayList<>();
        for (String required : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            return Component.literal("Blocked: missing " + String.join(", ", missing));
        }
        return Component.literal("Blocked: unknown requirement");
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
            if (i % 3 == 0) {
                graphics.fill(x, y + 1, x + 1, y + 2, 0x44222D3D);
            }
        }
    }

    private record NodeUiCache(
            String nodeId,
            ResourceLocation icon,
            boolean iconPresent,
            Component displayName,
            Component purposeLine,
            Component stateLine,
            Component effectLine,
            Component costLine,
            Component requirementLine,
            Component lockReasonLine
    ) {
        private NodeUiCache withState(SkillNodeStateService.NodeState state, Component reason) {
            String status = switch (state) {
                case UNLOCKED -> "State: UNLOCKED";
                case UNLOCKABLE -> "State: UNLOCKABLE";
                case LOCKED -> "State: LOCKED";
            };
            return new NodeUiCache(nodeId, icon, iconPresent, displayName, purposeLine, Component.literal(status), effectLine, costLine, requirementLine, reason);
        }
    }
}
