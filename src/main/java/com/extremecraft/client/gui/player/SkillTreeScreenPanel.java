package com.extremecraft.client.gui.player;

import com.extremecraft.network.ModNetwork;
import com.extremecraft.network.packet.UpgradeStatPacket;
import com.extremecraft.progression.capability.PlayerStatsCapability;
import com.extremecraft.progression.skilltree.SkillNode;
import com.extremecraft.progression.skilltree.SkillTreeManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class SkillTreeScreenPanel {
    private static final int NODE_SIZE = 18;

    private final Supplier<Optional<PlayerStatsCapability>> statsSupplier;

    public SkillTreeScreenPanel(Supplier<Optional<PlayerStatsCapability>> statsSupplier) {
        this.statsSupplier = statsSupplier;
    }

    public void render(GuiGraphics graphics, Font font, int panelLeft, int panelTop, int panelWidth, int panelHeight, String activeTreeId, int mouseX, int mouseY) {
        List<SkillNode> nodes = SkillTreeManager.nodesForTree(activeTreeId);
        if (nodes.isEmpty()) {
            graphics.drawString(font, Component.literal("No nodes in tree."), panelLeft + 8, panelTop + 8, 0xD5D9E2, false);
            return;
        }

        int centerX = panelLeft + panelWidth / 2;
        int centerY = panelTop + panelHeight / 2;

        for (SkillTreeManager.Connection connection : SkillTreeManager.connectionsForTree(activeTreeId)) {
            SkillNode from = SkillTreeManager.getNode(connection.from());
            SkillNode to = SkillTreeManager.getNode(connection.to());
            if (from == null || to == null) {
                continue;
            }

            int x0 = centerX + from.x();
            int y0 = centerY + from.y();
            int x1 = centerX + to.x();
            int y1 = centerY + to.y();
            drawLine(graphics, x0, y0, x1, y1, 0x885E6D83);
        }

        for (SkillNode node : nodes) {
            int x = centerX + node.x() - NODE_SIZE / 2;
            int y = centerY + node.y() - NODE_SIZE / 2;
            boolean unlocked = isUnlocked(node.id());
            boolean canUnlock = canUnlock(node);

            int fill = unlocked ? 0xCC5AA6FF : (canUnlock ? 0xAA6B5FA3 : 0xAA2E3140);
            graphics.fill(x, y, x + NODE_SIZE, y + NODE_SIZE, fill);
            graphics.drawCenteredString(font, Component.literal(unlocked ? "O" : "X"), x + NODE_SIZE / 2, y + 5, 0xFFE6ECF8);

            if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE) {
                graphics.renderTooltip(font, List.of(
                        Component.literal(node.id().replace('_', ' ')),
                        Component.literal(node.bonusText()),
                        Component.literal("Cost: " + node.cost() + " skill point(s)")
                ), Optional.empty(), mouseX, mouseY);
            }
        }
    }

    public boolean mouseClicked(int panelLeft, int panelTop, int panelWidth, int panelHeight, String activeTreeId, double mouseX, double mouseY) {
        List<SkillNode> nodes = SkillTreeManager.nodesForTree(activeTreeId);
        if (nodes.isEmpty()) {
            return false;
        }

        int centerX = panelLeft + panelWidth / 2;
        int centerY = panelTop + panelHeight / 2;

        for (SkillNode node : nodes) {
            int x = centerX + node.x() - NODE_SIZE / 2;
            int y = centerY + node.y() - NODE_SIZE / 2;

            if (mouseX >= x && mouseX <= x + NODE_SIZE && mouseY >= y && mouseY <= y + NODE_SIZE && canUnlock(node)) {
                ModNetwork.CHANNEL.sendToServer(new UpgradeStatPacket("skill:" + node.id()));
                return true;
            }
        }

        return false;
    }

    private boolean canUnlock(SkillNode node) {
        Optional<PlayerStatsCapability> statsOpt = statsSupplier.get();
        if (statsOpt.isEmpty()) {
            return false;
        }

        PlayerStatsCapability stats = statsOpt.get();
        if (stats.isSkillUnlocked(node.id()) || stats.skillPoints() < node.cost()) {
            return false;
        }

        for (String required : node.requiredNodes()) {
            if (!stats.isSkillUnlocked(required)) {
                return false;
            }
        }

        return true;
    }

    private boolean isUnlocked(String nodeId) {
        return statsSupplier.get().map(stats -> stats.isSkillUnlocked(nodeId)).orElse(false);
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
}
